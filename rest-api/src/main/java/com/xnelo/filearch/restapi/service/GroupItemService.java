package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
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
      final UserToken userInfo, final long groupId, final long fileId, final long tagId) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.TAG,
        ActionType.ASSIGN,
        user ->
            groupRepo
                .userActiveMemberInGroup(user.getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    ResourceType.TAG,
                                    ActionType.ASSIGN,
                                    ErrorCode.USER_NOT_ACTIVE,
                                    "User is not an active member of group(" + groupId + ").",
                                    400));
                      }

                      // TODO: Check permissions
                      return groupItemsRepo
                          .isItemInGroup(fileId, GroupItemType.FILE, groupId)
                          .chain(
                              itemInGroup -> {
                                if (!itemInGroup) {
                                  return Uni.createFrom()
                                      .item(
                                          Utils.createServiceErrorResponse(
                                              ResourceType.TAG,
                                              ActionType.ASSIGN,
                                              ErrorCode.ITEM_NOT_IN_GROUP,
                                              "File(" + fileId + ") not in group(" + groupId + ").",
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
                                                        ResourceType.TAG,
                                                        ActionType.ASSIGN,
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
                                                              ResourceType.TAG,
                                                              ActionType.ASSIGN,
                                                              success)));
                                        });
                              });
                    }));
  }

  public Uni<ServiceResponse<Boolean>> unassignTagFromGroupFile(
      final UserToken userInfo, final long groupId, final long fileId, final long tagId) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.TAG,
        ActionType.UNASSIGN,
        user ->
            groupRepo
                .userActiveMemberInGroup(user.getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    ResourceType.TAG,
                                    ActionType.UNASSIGN,
                                    ErrorCode.USER_NOT_ACTIVE,
                                    "User is not an active member of group(" + groupId + ").",
                                    400));
                      }
                      // TODO: Check Permissions
                      return fileTagsRepo
                          .unassignFileMapping(fileId, tagId, groupId)
                          .map(
                              success ->
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          ResourceType.TAG, ActionType.UNASSIGN, success)));
                    }));
  }
}
