package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.Group;
import com.xnelo.filearch.common.model.GroupFile;
import com.xnelo.filearch.common.model.GroupItem;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.model.GroupMember;
import com.xnelo.filearch.common.model.GroupMembershipStatus;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.api.contracts.GroupAddItemContract;
import com.xnelo.filearch.restapi.api.contracts.GroupAddUsersContract;
import com.xnelo.filearch.restapi.api.contracts.GroupCreateContract;
import com.xnelo.filearch.restapi.api.contracts.GroupItemContract;
import com.xnelo.filearch.restapi.api.contracts.GroupRemoveItemContract;
import com.xnelo.filearch.restapi.api.contracts.GroupRemoveUsersContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
import com.xnelo.filearch.restapi.data.GroupMemberPermissionsRepo;
import com.xnelo.filearch.restapi.data.GroupRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@RequestScoped
public class GroupService {
  public static final String GROUP_EXIST_KEY = "GROUP_EXIST_KEY__GROUP";

  @Inject UserService userService;
  @Inject GroupRepo groupRepo;
  @Inject GroupItemService groupItemService;
  @Inject GroupItemsRepo groupItemsRepo;
  @Inject GroupMemberPermissionsRepo groupMemberPermissionsRepo;
  @Inject GroupPermissionsService groupPermissionsService;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  public Uni<ServiceResponse<PaginatedResponse<Group>>> getAllGroups(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupRepo
                .getAll(context2.getUser().getId(), paginationParameters)
                .map(
                    paginatedGroups ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(),
                                context2.getActionType(),
                                paginationMapper.toPaginatedResponse(paginatedGroups)))));
  }

  public Uni<ServiceResponse<PaginatedResponse<Group>>> getGroupsIn(
      final ServiceRequestContext requestContext,
      final GroupMembershipStatus membershipStatus,
      final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupRepo
                .getGroupsIn(context2.getUser().getId(), membershipStatus, paginationParameters)
                .map(
                    paginatedGroups ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(),
                                context2.getActionType(),
                                paginationMapper.toPaginatedResponse(paginatedGroups)))));
  }

  public Uni<ServiceResponse<Group>> createNewGroup(
      final ServiceRequestContext requestContext, final GroupCreateContract newGroup) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupRepo
                .groupNameExists(context2.getUser().getId(), newGroup.getGroupName())
                .chain(
                    groupExists -> {
                      if (groupExists) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    requestContext,
                                    ErrorCode.GROUP_WITH_NAME_ALREADY_EXISTS,
                                    "A group with the name "
                                        + newGroup.getGroupName()
                                        + " already exists.",
                                    400));
                      }

                      return createGroupAndAddUser(context2, newGroup.getGroupName());
                    }));
  }

  Uni<ServiceResponse<Group>> createGroupAndAddUser(
      final ServiceRequestContext requestContext, final String groupName) {
    return groupRepo
        .createGroup(requestContext.getUser().getId(), groupName)
        .chain(
            createResponse -> {
              if (createResponse == null) {
                return Uni.createFrom()
                    .item(
                        Utils.createServiceErrorResponse(
                            requestContext,
                            ErrorCode.UNABLE_TO_CREATE_GROUP,
                            "Error creating group.",
                            500));
              }

              return groupRepo
                  .addUserToGroup(requestContext.getUser().getId(), createResponse.getId(), true)
                  .map(
                      memberAdded -> {
                        if (!memberAdded) {
                          return Utils.createServiceErrorResponse(
                              requestContext,
                              ErrorCode.UNABLE_TO_CREATE_GROUP,
                              "Error adding user as member to group.",
                              500);
                        }

                        return new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                createResponse));
                      });
            });
  }

  public Uni<ServiceResponse<Group>> deleteGroup(
      final ServiceRequestContext requestContext, final long groupId) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            checkGroupExists(
                context2,
                groupId,
                context3 ->
                    groupRepo
                        .deleteGroup(context3.getUser().getId(), groupId)
                        .chain(
                            deleteGroupResult -> {
                              if (deleteGroupResult == false) {
                                return Uni.createFrom()
                                    .item(
                                        Utils.createServiceErrorResponse(
                                            context3,
                                            ErrorCode.UNABLE_TO_DELETE_GROUP,
                                            "Error occurred while deleting group.",
                                            500));
                              }

                              return groupRepo
                                  .deleteAllUsersFromGroup(groupId)
                                  .chain(
                                      deleteGroupUsersResult -> {
                                        if (deleteGroupUsersResult == false) {
                                          return Uni.createFrom()
                                              .item(
                                                  Utils.createServiceErrorResponse(
                                                      context3,
                                                      ErrorCode.UNABLE_TO_DELETE_GROUP,
                                                      "Error occured while deleting group users.",
                                                      500));
                                        }

                                        return groupRepo
                                            .deleteAllItemsFromGroup(groupId)
                                            .chain(
                                                res -> {
                                                  if (res == false) {
                                                    return Uni.createFrom()
                                                        .item(
                                                            Utils.createServiceErrorResponse(
                                                                context3,
                                                                ErrorCode.UNABLE_TO_DELETE_GROUP,
                                                                "Error occurred while deleting group items.",
                                                                500));
                                                  }

                                                  return groupMemberPermissionsRepo
                                                      .deleteAllGroupPermissions(groupId)
                                                      .map(
                                                          permissionDeleteSuccess -> {
                                                            if (!permissionDeleteSuccess) {
                                                              return Utils
                                                                  .createServiceErrorResponse(
                                                                      context3,
                                                                      ErrorCode
                                                                          .UNABLE_TO_DELETE_GROUP_USER_PERMISSIONS,
                                                                      "Error while deleting group user permissions.",
                                                                      500);
                                                            }

                                                            Group groupInfo =
                                                                context3.getDataAs(
                                                                    GROUP_EXIST_KEY, Group.class);

                                                            return new ServiceResponse<>(
                                                                new ServiceActionResponse<>(
                                                                    context3.getResourceType(),
                                                                    context3.getActionType(),
                                                                    groupInfo));
                                                          });
                                                });
                                      });
                            })));
  }

  public Uni<ServiceResponse<String>> addUsersToGroup(
      final ServiceRequestContext requestContext,
      final long groupId,
      final GroupAddUsersContract usersToAdd) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupPermissionsService.userHasPermissionError(
                context2,
                groupId,
                GroupPermissionType.ADD_MEMBERS,
                () -> {
                  // if this is being executed then the group exists
                  ArrayList<Uni<ServiceActionResponse<String>>> individualUserAdds =
                      new ArrayList<>();
                  usersToAdd
                      .usersToAdd()
                      .forEach(
                          individualUser ->
                              individualUserAdds.add(
                                  addSingleUserToGroup(context2, groupId, individualUser)));
                  return Uni.combine()
                      .all()
                      .unis(individualUserAdds)
                      .with(
                          toCombine ->
                              Utils.combineServiceActionResponses(toCombine, String.class));
                }));
  }

  Uni<ServiceActionResponse<String>> addSingleUserToGroup(
      final ServiceRequestContext requestContext, final long groupId, final String username) {
    return userService
        .getUserByUsername(username)
        .chain(
            userResponse -> {
              if (userResponse.hasError()) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            requestContext.getResourceType(),
                            requestContext.getActionType(),
                            userResponse.getActionResponses().getFirst().getErrors()));
              }

              User userToAddData = userResponse.getActionResponses().getFirst().getData();

              return groupRepo
                  .userInGroup(userToAddData.getId(), groupId)
                  .chain(
                      userInGroup -> {
                        if (userInGroup != null && userInGroup) {
                          return Uni.createFrom()
                              .item(
                                  Utils.createServiceActionErrorResponse(
                                      requestContext,
                                      ErrorCode.UNABLE_TO_ADD_USER_TO_GROUP,
                                      "User ("
                                          + username
                                          + ") is already part of group ("
                                          + groupId
                                          + ")",
                                      400));
                        }

                        return groupRepo
                            .addUserToGroup(userToAddData.getId(), groupId, false)
                            .map(
                                successfullyAdded -> {
                                  if (successfullyAdded) {
                                    return new ServiceActionResponse<>(
                                        requestContext.getResourceType(),
                                        requestContext.getActionType(),
                                        username);
                                  } else {
                                    return new ServiceActionResponse<>(
                                        requestContext.getResourceType(),
                                        requestContext.getActionType(),
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.UNABLE_TO_ADD_USER_TO_GROUP)
                                                .errorMessage(
                                                    "Unable to add '" + username + "' to group.")
                                                .httpCode(500)
                                                .build()));
                                  }
                                });
                      });
            });
  }

  public Uni<ServiceResponse<Boolean>> acceptGroupInvitation(
      final ServiceRequestContext requestContext, final long groupId) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupRepo
                .acceptGroupInvite(context2.getUser().getId(), groupId)
                .map(
                    acceptSuccess -> {
                      if (!acceptSuccess) {
                        return Utils.createServiceErrorResponse(
                            context2,
                            ErrorCode.UNABLE_TO_ACCEPT_GROUP_INVITE,
                            "User is has not been invited to group.",
                            400);
                      } else {
                        return new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(),
                                context2.getActionType(),
                                true // This will always be true since we have an error message for
                                // false
                                ));
                      }
                    }));
  }

  public Uni<ServiceResponse<String>> removeUsersFromGroup(
      final ServiceRequestContext requestContext,
      final long groupId,
      final GroupRemoveUsersContract usersToRemove) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            groupPermissionsService.userHasPermissionError(
                requestContext,
                groupId,
                GroupPermissionType.REMOVE_MEMBERS,
                () ->
                    // if we are here then the group exists, and we have permissions in it.
                    groupRepo
                        .getOwnerOfGroup(groupId)
                        .chain(
                            groupOwnerId -> {
                              if (groupOwnerId == null) {
                                return Uni.createFrom()
                                    .item(
                                        Utils.createServiceErrorResponse(
                                            context2,
                                            ErrorCode.GROUP_DOES_NOT_EXIST,
                                            "Group does not exist",
                                            404));
                              }
                              ArrayList<Uni<ServiceActionResponse<String>>>
                                  individualRemoveUserUnis = new ArrayList<>();
                              usersToRemove
                                  .usersToRemove()
                                  .forEach(
                                      username ->
                                          individualRemoveUserUnis.add(
                                              removeIndividualUser(
                                                  context2, groupId, groupOwnerId, username)));
                              return Uni.combine()
                                  .all()
                                  .unis(individualRemoveUserUnis)
                                  .with(
                                      toCombine ->
                                          Utils.combineServiceActionResponses(
                                              toCombine, String.class));
                            })));
  }

  Uni<ServiceActionResponse<String>> removeIndividualUser(
      final ServiceRequestContext requestContext,
      final long groupId,
      final long groupOwnerId,
      final String username) {
    return userService
        .getUserByUsername(username)
        .chain(
            userServiceResponse -> {
              if (userServiceResponse.hasError()) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            requestContext.getResourceType(),
                            requestContext.getActionType(),
                            userServiceResponse.getActionResponses().getFirst().getErrors()));
              }

              User userToRemove = userServiceResponse.getActionResponses().getFirst().getData();

              if (userToRemove.getId() == groupOwnerId) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            requestContext.getResourceType(),
                            requestContext.getActionType(),
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.UNABLE_TO_REMOVE_USER_FROM_GROUP)
                                    .errorMessage("Cannot remove owner of group from group.")
                                    .httpCode(400)
                                    .build())));
              }

              return groupRepo
                  .removeUserFromGroup(userToRemove.getId(), groupId)
                  .chain(
                      successful -> {
                        if (!successful) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceActionResponse<>(
                                      requestContext.getResourceType(),
                                      requestContext.getActionType(),
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(ErrorCode.UNABLE_TO_REMOVE_USER_FROM_GROUP)
                                              .errorMessage(
                                                  "Error while removing user("
                                                      + username
                                                      + ") from group ("
                                                      + groupId
                                                      + ").")
                                              .httpCode(500)
                                              .build())));
                        } else {

                          return groupMemberPermissionsRepo
                              .deleteUserPermissionsFromGroup(userToRemove.getId(), groupId)
                              .map(
                                  deletePermissionsSuccess -> {
                                    if (!deletePermissionsSuccess) {
                                      return new ServiceActionResponse<>(
                                          requestContext.getResourceType(),
                                          requestContext.getActionType(),
                                          List.of(
                                              ServiceError.builder()
                                                  .errorCode(
                                                      ErrorCode
                                                          .UNABLE_TO_DELETE_GROUP_USER_PERMISSIONS)
                                                  .errorMessage(
                                                      "Unable to remove user permissions from group.")
                                                  .httpCode(500)
                                                  .build()));
                                    }

                                    return new ServiceActionResponse<>(
                                        requestContext.getResourceType(),
                                        requestContext.getActionType(),
                                        username);
                                  });
                        }
                      });
            });
  }

  public Uni<ServiceResponse<GroupItem>> addItemsToGroup(
      final ServiceRequestContext requestContext,
      final long groupId,
      final GroupAddItemContract itemsToAdd) {
    // Step 1: Check user exists
    return userService.checkUserExist(
        requestContext,
        context2 ->
            // Step 2: check user is member of group and accepted
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
                                    "User ("
                                        + context2.getUser().getId()
                                        + ") is not active in group ("
                                        + groupId
                                        + "). Check that the group exists and user is active member. ",
                                    400));
                      }

                      // step 3: Check user has permission to add
                      // step 4: iterate over each item and add them individually
                      return groupPermissionsService.userHasPermissionError(
                          requestContext,
                          groupId,
                          GroupPermissionType.ADD_ITEMS,
                          () -> addEachItemIndividually(context2, itemsToAdd, groupId));
                    }));
  }

  Uni<ServiceResponse<GroupItem>> addEachItemIndividually(
      final ServiceRequestContext requestContext,
      final GroupAddItemContract itemsToAdd,
      final long groupId) {
    ArrayList<Uni<ServiceActionResponse<GroupItem>>> individualItemAdd = new ArrayList<>();

    itemsToAdd
        .itemsToAdd()
        .forEach(
            itemContract ->
                individualItemAdd.add(individualAddItem(requestContext, itemContract, groupId)));

    return Uni.combine()
        .all()
        .unis(individualItemAdd)
        .with(toCombine -> Utils.combineServiceActionResponses(toCombine, GroupItem.class));
  }

  Uni<ServiceActionResponse<GroupItem>> individualAddItem(
      final ServiceRequestContext requestContext,
      final GroupItemContract itemToAdd,
      final long groupId) {
    // Step 1: Check that the item id is not null and < 0
    if (itemToAdd.itemId() < 0) {
      return Uni.createFrom()
          .item(
              Utils.createServiceActionErrorResponse(
                  requestContext,
                  ErrorCode.INVALID_INPUT_VALUE,
                  "Item id must be 0 or greater.",
                  400));
    }

    // Step 2: Check that the item type is not null and not UNKNOWN
    if (itemToAdd.itemType() == null || itemToAdd.itemType().equals(GroupItemType.UNKNOWN)) {
      return Uni.createFrom()
          .item(
              Utils.createServiceActionErrorResponse(
                  requestContext,
                  ErrorCode.INVALID_INPUT_VALUE,
                  "Item type must be present and NOT UNKNOWN.",
                  400));
    }

    // Step 3: Check item exists
    return groupItemService
        .checkItemExists(requestContext, itemToAdd.itemType(), itemToAdd.itemId())
        .chain(
            itemExist -> {
              if (!itemExist) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            requestContext.getResourceType(),
                            requestContext.getActionType(),
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.UNABLE_TO_ADD_ITEM_TO_GROUP)
                                    .errorMessage(
                                        "Unable to add item ("
                                            + itemToAdd.itemId()
                                            + " - "
                                            + groupId
                                            + ")")
                                    .httpCode(400)
                                    .build())));
              }

              return groupItemsRepo
                  .isItemInGroup(itemToAdd.itemId(), itemToAdd.itemType(), groupId)
                  .chain(
                      isItemInGroup -> {
                        if (isItemInGroup) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceActionResponse<>(
                                      requestContext.getResourceType(),
                                      requestContext.getActionType(),
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(ErrorCode.ITEM_ALREADY_IN_GROUP)
                                              .errorMessage("Item already part of group.")
                                              .httpCode(400)
                                              .build())));
                        }

                        // Step 4: add item to group items table
                        return groupItemsRepo
                            .addItemToGroup(itemToAdd.itemId(), itemToAdd.itemType(), groupId)
                            .map(
                                newGroupItem -> {
                                  if (newGroupItem == null) {
                                    return new ServiceActionResponse<>(
                                        requestContext.getResourceType(),
                                        requestContext.getActionType(),
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.UNABLE_TO_ADD_ITEM_TO_GROUP)
                                                .errorMessage(
                                                    "Error inserting item to DB. Contact support.")
                                                .httpCode(500)
                                                .build()));
                                  }

                                  return new ServiceActionResponse<>(
                                      requestContext.getResourceType(),
                                      requestContext.getActionType(),
                                      newGroupItem);
                                });
                      });
            });
  }

  public Uni<ServiceResponse<GroupItem>> removeItemsFromGroup(
      final ServiceRequestContext requestContext,
      final long groupId,
      final GroupRemoveItemContract itemsToRemove) {
    // Step 1: Check user exists
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
                                    "User ("
                                        + context2.getUser().getId()
                                        + ") is not active in group ("
                                        + groupId
                                        + "). Check that the group exists and user is active member. ",
                                    400));
                      }

                      // step 3: Check user has permission to add
                      // step 4: iterate over each item and add them individually
                      return groupPermissionsService.userHasPermissionError(
                          requestContext,
                          groupId,
                          GroupPermissionType.REMOVE_ITEMS,
                          () -> removeEachItemIndividually(context2, itemsToRemove, groupId));
                    }));
  }

  Uni<ServiceResponse<GroupItem>> removeEachItemIndividually(
      final ServiceRequestContext requestContext,
      final GroupRemoveItemContract itemsToRemove,
      final long groupId) {
    ArrayList<Uni<ServiceActionResponse<GroupItem>>> individualItemRemove = new ArrayList<>();

    itemsToRemove
        .itemsToRemove()
        .forEach(
            itemContract ->
                individualItemRemove.add(
                    individualRemoveItem(requestContext, itemContract, groupId)));

    return Uni.combine()
        .all()
        .unis(individualItemRemove)
        .with(toCombine -> Utils.combineServiceActionResponses(toCombine, GroupItem.class));
  }

  Uni<ServiceActionResponse<GroupItem>> individualRemoveItem(
      final ServiceRequestContext requestContext,
      final GroupItemContract itemToRemove,
      final long groupId) {
    // Step 1: Check that the item id is not null and < 0
    if (itemToRemove.itemId() < 0) {
      return Uni.createFrom()
          .item(
              new ServiceActionResponse<>(
                  requestContext.getResourceType(),
                  requestContext.getActionType(),
                  List.of(
                      ServiceError.builder()
                          .errorCode(ErrorCode.INVALID_INPUT_VALUE)
                          .errorMessage("Item id must be 0 or greater.")
                          .httpCode(400)
                          .build())));
    }

    // Step 2: Check that the item type is not null and not UNKNOWN
    if (itemToRemove.itemType() == null || itemToRemove.itemType().equals(GroupItemType.UNKNOWN)) {
      return Uni.createFrom()
          .item(
              new ServiceActionResponse<>(
                  requestContext.getResourceType(),
                  requestContext.getActionType(),
                  List.of(
                      ServiceError.builder()
                          .errorCode(ErrorCode.INVALID_INPUT_VALUE)
                          .errorMessage("Item type must be present and NOT UNKNOWN.")
                          .httpCode(400)
                          .build())));
    }

    return groupItemsRepo
        .removeItemFromGroup(itemToRemove.itemId(), itemToRemove.itemType(), groupId)
        .map(
            returnedGroupItem -> {
              if (returnedGroupItem == null) {
                return new ServiceActionResponse<>(
                    requestContext.getResourceType(),
                    requestContext.getActionType(),
                    List.of(
                        ServiceError.builder()
                            .errorCode(ErrorCode.UNABLE_TO_REMOVE_ITEM_FROM_GROUP)
                            .errorMessage(
                                "Unable to remove item("
                                    + itemToRemove.itemId()
                                    + ") of type("
                                    + itemToRemove.itemType()
                                    + ") from group ("
                                    + groupId
                                    + ").")
                            .httpCode(500)
                            .build()));
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(),
                  requestContext.getActionType(),
                  returnedGroupItem);
            });
  }

  public Uni<ServiceResponse<List<GroupMember>>> getUsersInGroup(
      final ServiceRequestContext requestContext, final long groupId) {
    // Step 1: Check user exists
    return userService.checkUserExist(
        requestContext,
        // Step 2: Check user active member of group
        context2 ->
            groupRepo
                .userActiveMemberInGroup(context2.getUser().getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        context2.getResourceType(),
                                        context2.getActionType(),
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                                .errorMessage(
                                                    "User is not an active member of this group.")
                                                .httpCode(400)
                                                .build()))));
                      }

                      // Step 3 get all group member info
                      return groupRepo
                          .getUsersInGroup(groupId)
                          .map(
                              groupMembersList ->
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          context2.getResourceType(),
                                          context2.getActionType(),
                                          groupMembersList)));
                    }));
  }

  public Uni<ServiceResponse<PaginatedResponse<GroupFile>>> getFilesInGroup(
      final ServiceRequestContext requestContext,
      final long groupId,
      final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

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
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        context2.getResourceType(),
                                        context2.getActionType(),
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                                .errorMessage(
                                                    "User ("
                                                        + context2.getUser().getId()
                                                        + ") is not an active member of this group ("
                                                        + groupId
                                                        + ").")
                                                .httpCode(400)
                                                .build()))));
                      }

                      return groupRepo
                          .getFilesInGroup(groupId, paginationParameters)
                          .map(
                              data ->
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          context2.getResourceType(),
                                          context2.getActionType(),
                                          paginationMapper.toPaginatedResponse(data))));
                    }));
  }

  <T> Uni<ServiceResponse<T>> checkUserActiveMemberInGroup(
      final ServiceRequestContext requestContext,
      final Function<ServiceRequestContext, Uni<ServiceResponse<T>>> activeMemberAction) {
    return groupRepo
        .userActiveMemberInGroup(requestContext.getUser().getId(), requestContext.getGroupId())
        .chain(
            isActiveMember -> {
              if (!isActiveMember) {
                return Uni.createFrom()
                    .item(
                        Utils.createServiceErrorResponse(
                            requestContext,
                            ErrorCode.USER_NOT_ACTIVE,
                            "User ("
                                + requestContext.getUser().getId()
                                + ") is not an active member of group ("
                                + requestContext.getGroupId()
                                + ").",
                            403));
              }

              return activeMemberAction.apply(requestContext);
            });
  }

  <T> Uni<ServiceResponse<T>> checkGroupExists(
      final ServiceRequestContext requestContext,
      final long groupId,
      final Function<ServiceRequestContext, Uni<ServiceResponse<T>>> groupExistAction) {
    return groupRepo
        .getGroupById(requestContext.getUser().getId(), groupId)
        .chain(
            group -> {
              if (group == null) {
                return Uni.createFrom()
                    .item(
                        Utils.createServiceErrorResponse(
                            requestContext,
                            ErrorCode.GROUP_DOES_NOT_EXIST,
                            "Group (" + groupId + ") does not exist.",
                            404));
              }

              requestContext.setData(GROUP_EXIST_KEY, group);

              return groupExistAction.apply(requestContext);
            });
  }

  Uni<ServiceRequestContext> checkUserActiveMember(ServiceRequestContext requestContext) {
    Utils.checkUserInRequest(requestContext);
    Utils.checkGroupInRequest(requestContext);

    return groupRepo
        .userActiveMemberInGroup(requestContext.getUser().getId(), requestContext.getGroupId())
        .map(
            res -> {
              if (!res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.USER_NOT_ACTIVE,
                    "User is not an active member of group(" + requestContext.getGroupId() + ").",
                    400);
              }

              return requestContext;
            });
  }
}
