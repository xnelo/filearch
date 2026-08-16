package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.data.FileTagsRepo;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
import com.xnelo.filearch.restapi.data.GroupRepo;
import com.xnelo.filearch.restapi.data.SharedTagsRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class GroupItemService {
  @Inject StoredFilesRepo storedFilesRepo;
  @Inject FolderRepo folderRepo;
  @Inject UserService userService;
  @Inject GroupRepo groupRepo;
  @Inject GroupItemsRepo groupItemsRepo;
  @Inject SharedTagsRepo sharedTagsRepo;
  @Inject FileTagsRepo fileTagsRepo;
  @Inject GroupPermissionsService groupPermissionsService;

  public Uni<Boolean> checkItemExists(
      final ServiceRequestContext requestContext, final GroupItemType itemType, final long itemId) {
    if (itemType == null) {
      log.warn("Invalid input: item type is null.");
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.INVALID_INPUT_VALUE,
          "Item type cannot be null",
          400); // "Item ([" + itemType + "] " + itemId + ") does not exist.", 404);
    } else if (itemId < 0) {
      log.warn("Invalid input: itemId is negative.");
      throw new ServiceResponseException(
          requestContext, ErrorCode.INVALID_INPUT_VALUE, "Item ID cannot be negative", 400);
    }

    return switch (itemType) {
      case FILE -> fileItemExists(itemId, requestContext.getUser().getId());
      case FOLDER -> folderItemExists(itemId, requestContext.getUser().getId());
      case UNKNOWN -> {
        log.warn("Invalid input: itemType is unknown.");
        throw new ServiceResponseException(
            requestContext, ErrorCode.INVALID_INPUT_VALUE, "Item type cannot be UNKNOWN", 400);
      }
    };
  }

  @Deprecated
  public Uni<Boolean> itemExists(
      final GroupItemType itemType, final long itemId, final long userId) {
    if (itemType == null) {
      log.warn("Invalid input: item type is null.");
      return Uni.createFrom().item(Boolean.FALSE);
    } else if (itemId < 0) {
      log.warn("Invalid input: itemId is negative.");
      return Uni.createFrom().item(Boolean.FALSE);
    }

    return switch (itemType) {
      case FILE -> fileItemExists(itemId, userId);
      case FOLDER -> folderItemExists(itemId, userId);
      case UNKNOWN -> {
        log.warn("Invalid input: itemType is unknown.");
        yield Uni.createFrom().item(Boolean.FALSE);
      }
    };
  }

  private Uni<Boolean> fileItemExists(final long fileId, final long userId) {
    return storedFilesRepo
        .getStoredFile(fileId, userId)
        .map(Objects::nonNull)
        .onFailure()
        .invoke(ex -> log.error("Error retrieving file({}) ", fileId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  private Uni<Boolean> folderItemExists(final long folderId, final long userId) {
    return folderRepo
        .getFolderById(folderId, userId)
        .map(Objects::nonNull)
        .onFailure()
        .invoke(ex -> log.error("Error retrieving folder({}) ", folderId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<ServiceResponse<Boolean>> assignTagToGroupFile(
      final ServiceRequestContext requestContext,
      final long groupId,
      final long fileId,
      final long tagId) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupRepo
                .userActiveMemberInGroup(context2.getUser().getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    context2,
                                    ErrorCode.USER_NOT_ACTIVE,
                                    "User is not an active member of group(" + groupId + ").",
                                    400));
                      }

                      return groupPermissionsService.userHasPermissionError(
                          context2,
                          groupId,
                          GroupPermissionType.TAG_ITEMS,
                          () ->
                              groupItemsRepo
                                  .isItemInGroup(fileId, GroupItemType.FILE, groupId)
                                  .chain(
                                      itemInGroup -> {
                                        if (!itemInGroup) {
                                          return Uni.createFrom()
                                              .item(
                                                  Utils.createServiceErrorResponse(
                                                      context2,
                                                      ErrorCode.ITEM_NOT_IN_GROUP,
                                                      "File("
                                                          + fileId
                                                          + ") not in group("
                                                          + groupId
                                                          + ").",
                                                      404));
                                        }

                                        return sharedTagsRepo
                                            .tagShareExists(tagId, groupId)
                                            .chain(
                                                tagShareExists -> {
                                                  if (!tagShareExists) {
                                                    return Uni.createFrom()
                                                        .item(
                                                            Utils.createServiceErrorResponse(
                                                                context2,
                                                                ErrorCode.TAG_SHARE_DOES_NOT_EXIST,
                                                                "Tag("
                                                                    + tagId
                                                                    + ") is not shared with group("
                                                                    + groupId
                                                                    + ").",
                                                                400));
                                                  }

                                                  return fileTagsRepo
                                                      .assignFileMapping(fileId, tagId, groupId)
                                                      .map(
                                                          success ->
                                                              new ServiceResponse<>(
                                                                  new ServiceActionResponse<>(
                                                                      context2.getResourceType(),
                                                                      context2.getActionType(),
                                                                      success)));
                                                });
                                      }));
                    }));
  }

  public Uni<ServiceResponse<Boolean>> unassignTagFromGroupFile(
      final ServiceRequestContext requestContext,
      final long groupId,
      final long fileId,
      final long tagId) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupRepo
                .userActiveMemberInGroup(context2.getUser().getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    context2,
                                    ErrorCode.USER_NOT_ACTIVE,
                                    "User is not an active member of group(" + groupId + ").",
                                    400));
                      }

                      return groupPermissionsService
                          .userHasPermission(
                              context2.getUser().getId(), groupId, GroupPermissionType.REMOVE_TAGS)
                          .chain(
                              hasPermission -> {
                                if (!hasPermission) {
                                  return Uni.createFrom()
                                      .item(
                                          Utils.createServiceErrorResponse(
                                              context2,
                                              ErrorCode.PERMISSION_NOT_GRANTED,
                                              "User does not have permission to remove tags.",
                                              403));
                                }

                                return fileTagsRepo
                                    .unassignFileMapping(fileId, tagId, groupId)
                                    .map(
                                        success ->
                                            new ServiceResponse<>(
                                                new ServiceActionResponse<>(
                                                    context2.getResourceType(),
                                                    context2.getActionType(),
                                                    success)));
                              });
                    }));
  }

  <T> Uni<ServiceResponse<T>> checkItemInGroup(
      final ServiceRequestContext requestContext,
      final long itemId,
      final GroupItemType itemType,
      final Function<ServiceRequestContext, Uni<ServiceResponse<T>>> itemInGroupAction) {
    return groupItemsRepo
        .isItemInGroup(itemId, itemType, requestContext.getGroupId())
        .chain(
            isItemInGroup -> {
              if (!isItemInGroup) {
                return Uni.createFrom()
                    .item(
                        Utils.createServiceErrorResponse(
                            requestContext,
                            ErrorCode.ITEM_NOT_IN_GROUP,
                            "Item ("
                                + itemId
                                + " - "
                                + itemType
                                + ") is not in the group ("
                                + requestContext.getGroupId()
                                + ").",
                            403));
              }

              return itemInGroupAction.apply(requestContext);
            });
  }
}
