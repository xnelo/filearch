package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class GroupItemService {
  private final FolderRepo folderRepo;
  private final GroupItemsRepo groupItemsRepo;
  private final StoredFilesRepo storedFilesRepo;

  @Inject
  public GroupItemService(
      final FolderRepo folderRepo,
      final GroupItemsRepo groupItemsRepo,
      final StoredFilesRepo storedFilesRepo) {
    this.folderRepo = folderRepo;
    this.groupItemsRepo = groupItemsRepo;
    this.storedFilesRepo = storedFilesRepo;
  }

  /**
   * Check that an item exists based on the item ID and the Item type. If it doesn't exist then an
   * exception is thrown.
   *
   * @param requestContext The context for the call. It must contain group ID information.
   * @param itemType The type of the item to search for.
   * @param itemId The id of the item to search for.
   * @return The request context for chaining unis.
   * @throws ServiceResponseException If the item does not exist or the input is invalid.
   * @apiNote Do not use this to check if an item is in a group use {@link
   *     #checkItemInGroup(ServiceRequestContext, long, GroupItemType)} method.
   */
  public Uni<ServiceRequestContext> checkItemExistsThrowError(
      final ServiceRequestContext requestContext, final GroupItemType itemType, final long itemId) {
    return checkItemExists(requestContext, itemType, itemId)
        .map(
            itemExists -> {
              if (!itemExists) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.ITEM_DOES_NOT_EXIST,
                    "Item ([" + itemType + "] " + itemId + ") does not exist.",
                    404);
              }

              return requestContext;
            });
  }

  /**
   * Check that an item exists based on the item ID and the Item type.
   *
   * @param requestContext The context for the call. It must contain group ID information.
   * @param itemType The type of the item to search for.
   * @param itemId The id of the item to search for.
   * @return True if the item exists. False if it doesn't.
   * @throws ServiceResponseException If the input is invalid.
   * @apiNote Do not use this to check if an item is in a group use {@link
   *     #checkItemInGroup(ServiceRequestContext, long, GroupItemType)} method.
   */
  public Uni<Boolean> checkItemExists(
      final ServiceRequestContext requestContext, final GroupItemType itemType, final long itemId) {
    if (itemType == null) {
      log.warn("Invalid input: item type is null.");
      throw new ServiceResponseException(
          requestContext, ErrorCode.INVALID_INPUT_VALUE, "Item type cannot be null", 400);
    } else if (itemId < 0) {
      log.warn("Invalid input: itemId is negative.");
      throw new ServiceResponseException(
          requestContext, ErrorCode.INVALID_INPUT_VALUE, "Item ID cannot be negative", 400);
    }

    Utils.checkUserInRequest(requestContext);

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

  public Uni<ServiceRequestContext> checkItemNotInGroup(
      final ServiceRequestContext requestContext, final long itemId, final GroupItemType itemType) {
    Utils.checkGroupInRequest(requestContext);

    return groupItemsRepo
        .isItemInGroup(itemId, itemType, requestContext.getGroupId())
        .map(
            res -> {
              if (res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.ITEM_ALREADY_IN_GROUP,
                    "Item ("
                        + itemId
                        + " - "
                        + itemType
                        + ") is already in the group ("
                        + requestContext.getGroupId()
                        + ").",
                    400);
              }

              return requestContext;
            });
  }

  /**
   * Checks if an item is part of the group specified in the request context. If it is not then an
   * exception is thrown.
   *
   * @param requestContext The request context with data. The request context must have group
   *     information.
   * @param itemId The item id number.
   * @param itemType The item type
   * @return the request context for chaining unis
   * @throws ServiceResponseException If the item is not part of the group specified in the request
   *     context.
   */
  public Uni<ServiceRequestContext> checkItemInGroup(
      ServiceRequestContext requestContext, final long itemId, final GroupItemType itemType) {
    Utils.checkGroupInRequest(requestContext);

    return groupItemsRepo
        .isItemInGroup(itemId, itemType, requestContext.getGroupId())
        .map(
            res -> {
              if (!res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.ITEM_NOT_IN_GROUP,
                    "Item ("
                        + itemId
                        + " - "
                        + itemType
                        + ") is not in the group ("
                        + requestContext.getGroupId()
                        + ").",
                    404);
              }

              return requestContext;
            });
  }
}
