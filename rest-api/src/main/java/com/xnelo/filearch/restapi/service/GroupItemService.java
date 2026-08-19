package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
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
  @Inject GroupItemsRepo groupItemsRepo;

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

  Uni<ServiceRequestContext> checkItemInGroup(
      ServiceRequestContext requestContext, final long itemId, final GroupItemType itemType) {
    Utils.checkGroupInRequest(requestContext);

    return groupItemsRepo
        .isItemInGroup(itemId, itemType, requestContext.getGroupId())
        .map(
            res -> {
              if (!res) {
                throw new ServiceResponseException(
                    requestContext, ErrorCode.ITEM_NOT_IN_GROUP, "Group Item does not exist.", 404);
              }

              return requestContext;
            });
  }
}
