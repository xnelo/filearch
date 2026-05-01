package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.Group;
import com.xnelo.filearch.common.model.GroupItem;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.model.GroupMembershipStatus;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
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
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@RequestScoped
public class GroupService {
  @Inject UserService userService;
  @Inject GroupRepo groupRepo;
  @Inject GroupItemService groupItemService;
  @Inject GroupItemsRepo groupItemsRepo;
  @Inject GroupMemberPermissionsRepo groupMemberPermissionsRepo;
  @Inject GroupPermissionsService groupPermissionsService;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  public Uni<ServiceResponse<PaginatedResponse<Group>>> getAllGroups(
      final UserToken userInfo, final PaginationParameters paginationParameters) {
    ServiceResponse<PaginatedResponse<Group>> response;
    response =
        Utils.validatePaginationParameters(
            paginationParameters, ResourceType.GROUP, ActionType.GET);
    if (response != null) {
      return Uni.createFrom().item(response);
    }

    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.GET,
        user ->
            groupRepo
                .getAll(user.getId(), paginationParameters)
                .map(
                    paginatedGroups ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.GROUP,
                                ActionType.GET,
                                paginationMapper.toPaginatedResponse(paginatedGroups)))));
  }

  public Uni<ServiceResponse<PaginatedResponse<Group>>> getGroupsIn(
      final UserToken userInfo,
      final GroupMembershipStatus membershipStatus,
      final PaginationParameters paginationParameters) {
    ServiceResponse<PaginatedResponse<Group>> response;
    response =
        Utils.validatePaginationParameters(
            paginationParameters, ResourceType.GROUP, ActionType.GET);
    if (response != null) {
      return Uni.createFrom().item(response);
    }

    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.GET,
        user ->
            groupRepo
                .getGroupsIn(user.getId(), membershipStatus, paginationParameters)
                .map(
                    paginatedGroups ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.GROUP,
                                ActionType.GET,
                                paginationMapper.toPaginatedResponse(paginatedGroups)))));
  }

  public Uni<ServiceResponse<Group>> createNewGroup(
      final GroupCreateContract newGroup, final UserToken userToken) {
    return userService.checkUserExist(
        userToken,
        ResourceType.GROUP,
        ActionType.CREATE,
        user ->
            groupRepo
                .groupNameExists(user.getId(), newGroup.getGroupName())
                .chain(
                    groupExists -> {
                      if (groupExists) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.GROUP,
                                        ActionType.CREATE,
                                        List.of(
                                            ServiceError.builder()
                                                .httpCode(400)
                                                .errorCode(ErrorCode.GROUP_WITH_NAME_ALREADY_EXISTS)
                                                .errorMessage(
                                                    "A group with the name "
                                                        + newGroup.getGroupName()
                                                        + " already exists.")
                                                .build()))));
                      }

                      return createGroupAndAddUser(user.getId(), newGroup.getGroupName());
                    }));
  }

  Uni<ServiceResponse<Group>> createGroupAndAddUser(final long userId, final String groupName) {
    return groupRepo
        .createGroup(userId, groupName)
        .chain(
            createResponse -> {
              if (createResponse == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.GROUP,
                                ActionType.CREATE,
                                List.of(
                                    ServiceError.builder()
                                        .errorMessage("Error creating group.")
                                        .errorCode(ErrorCode.UNABLE_TO_CREATE_GROUP)
                                        .httpCode(500)
                                        .build()))));
              }

              return groupRepo
                  .addUserToGroup(userId, createResponse.getId(), true)
                  .map(
                      memberAdded -> {
                        if (!memberAdded) {
                          return new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.GROUP,
                                  ActionType.CREATE,
                                  List.of(
                                      ServiceError.builder()
                                          .errorMessage("Error adding user as member to group.")
                                          .errorCode(ErrorCode.UNABLE_TO_CREATE_GROUP)
                                          .httpCode(500)
                                          .build())));
                        }

                        return new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.GROUP, ActionType.CREATE, createResponse));
                      });
            });
  }

  public Uni<ServiceResponse<Group>> getGroupById(final long userId, final long groupId) {
    return groupRepo
        .getGroupById(userId, groupId)
        .map(
            group -> {
              if (group == null) {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        ResourceType.GROUP,
                        ActionType.GET,
                        List.of(
                            ServiceError.builder()
                                .errorCode(ErrorCode.GROUP_DOES_NOT_EXIST)
                                .errorMessage("Group does not exist.")
                                .httpCode(404)
                                .build())));
              }

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(ResourceType.GROUP, ActionType.GET, group));
            });
  }

  public Uni<ServiceResponse<Group>> deleteGroup(final UserToken userInfo, final long groupId) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.DELETE,
        user ->
            getGroupById(user.getId(), groupId)
                .chain(
                    groupServiceResponse -> {
                      if (groupServiceResponse.hasError()) {
                        return Uni.createFrom()
                            .item(
                                Utils.updateErrorAndPassThrough(
                                    groupServiceResponse, ResourceType.GROUP, ActionType.DELETE));
                      }
                      return groupRepo
                          .deleteGroup(user.getId(), groupId)
                          .chain(
                              deleteGroupResult -> {
                                if (deleteGroupResult == false) {
                                  return Uni.createFrom()
                                      .item(
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  ResourceType.GROUP,
                                                  ActionType.DELETE,
                                                  List.of(
                                                      ServiceError.builder()
                                                          .errorCode(
                                                              ErrorCode.UNABLE_TO_DELETE_GROUP)
                                                          .errorMessage(
                                                              "Error occurred while deleting group.")
                                                          .httpCode(500)
                                                          .build()))));
                                }

                                return groupRepo
                                    .deleteAllUsersFromGroup(groupId)
                                    .chain(
                                        deleteGroupUsersResult -> {
                                          if (deleteGroupUsersResult == false) {
                                            return Uni.createFrom()
                                                .item(
                                                    new ServiceResponse<>(
                                                        new ServiceActionResponse<>(
                                                            ResourceType.GROUP,
                                                            ActionType.DELETE,
                                                            List.of(
                                                                ServiceError.builder()
                                                                    .errorCode(
                                                                        ErrorCode
                                                                            .UNABLE_TO_DELETE_GROUP)
                                                                    .errorMessage(
                                                                        "Error occured while deleting group users.")
                                                                    .httpCode(500)
                                                                    .build()))));
                                          }

                                          return groupRepo
                                              .deleteAllItemsFromGroup(groupId)
                                              .chain(
                                                  res -> {
                                                    if (res == false) {
                                                      return Uni.createFrom()
                                                          .item(
                                                              new ServiceResponse<>(
                                                                  new ServiceActionResponse<>(
                                                                      ResourceType.GROUP,
                                                                      ActionType.DELETE,
                                                                      List.of(
                                                                          ServiceError.builder()
                                                                              .errorCode(
                                                                                  ErrorCode
                                                                                      .UNABLE_TO_DELETE_GROUP)
                                                                              .errorMessage(
                                                                                  "Error occurred while deleting group items.")
                                                                              .httpCode(500)
                                                                              .build()))));
                                                    }

                                                    return groupMemberPermissionsRepo
                                                        .deleteAllGroupPermissions(groupId)
                                                        .map(
                                                            permissionDeleteSuccess -> {
                                                              if (!permissionDeleteSuccess) {
                                                                return new ServiceResponse<>(
                                                                    new ServiceActionResponse<>(
                                                                        ResourceType.GROUP,
                                                                        ActionType.DELETE,
                                                                        List.of(
                                                                            ServiceError.builder()
                                                                                .errorCode(
                                                                                    ErrorCode
                                                                                        .UNABLE_TO_DELETE_GROUP_USER_PERMISSIONS)
                                                                                .errorMessage(
                                                                                    "Error while deleting group user permissions.")
                                                                                .httpCode(500)
                                                                                .build())));
                                                              }

                                                              return new ServiceResponse<>(
                                                                  new ServiceActionResponse<>(
                                                                      ResourceType.GROUP,
                                                                      ActionType.DELETE,
                                                                      groupServiceResponse
                                                                          .getActionResponses()
                                                                          .getFirst()
                                                                          .getData()));
                                                            });
                                                  });
                                        });
                              });
                    }));
  }

  public Uni<ServiceResponse<String>> addUsersToGroup(
      final UserToken userInfo, final long groupId, final GroupAddUsersContract usersToAdd) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.ADD_USER_TO_GROUP,
        user ->
            groupPermissionsService.userHasPermissionError(
                ResourceType.GROUP,
                ActionType.ADD_USER_TO_GROUP,
                user.getId(),
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
                                  addSingleUserToGroup(groupId, individualUser)));
                  return Uni.combine()
                      .all()
                      .unis(individualUserAdds)
                      .with(
                          toCombine ->
                              Utils.combineServiceActionResponses(toCombine, String.class));
                }));
  }

  Uni<ServiceActionResponse<String>> addSingleUserToGroup(
      final long groupId, final String username) {
    return userService
        .getUserByUsername(username)
        .chain(
            userResponse -> {
              if (userResponse.hasError()) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            ResourceType.GROUP,
                            ActionType.ADD_USER_TO_GROUP,
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
                                  new ServiceActionResponse<>(
                                      ResourceType.GROUP,
                                      ActionType.ADD_USER_TO_GROUP,
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(ErrorCode.UNABLE_TO_ADD_USER_TO_GROUP)
                                              .errorMessage(
                                                  "User ("
                                                      + username
                                                      + ") is already part of group ("
                                                      + groupId
                                                      + ")")
                                              .httpCode(400)
                                              .build())));
                        }

                        return groupRepo
                            .addUserToGroup(userToAddData.getId(), groupId, false)
                            .map(
                                successfullyAdded -> {
                                  if (successfullyAdded) {
                                    return new ServiceActionResponse<>(
                                        ResourceType.GROUP, ActionType.ADD_USER_TO_GROUP, username);
                                  } else {
                                    return new ServiceActionResponse<>(
                                        ResourceType.GROUP,
                                        ActionType.ADD_USER_TO_GROUP,
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
      final UserToken userInfo, final long groupId) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.ACCEPT_GROUP_INVITE,
        user ->
            groupRepo
                .acceptGroupInvite(user.getId(), groupId)
                .map(
                    acceptSuccess -> {
                      if (!acceptSuccess) {
                        return new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.GROUP,
                                ActionType.ACCEPT_GROUP_INVITE,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.UNABLE_TO_ACCEPT_GROUP_INVITE)
                                        .errorMessage("User is has not been invited to group.")
                                        .httpCode(400)
                                        .build())));
                      } else {
                        return new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.GROUP,
                                ActionType.ACCEPT_GROUP_INVITE,
                                true // This will always be true since we have an error message for
                                // false
                                ));
                      }
                    }));
  }

  public Uni<ServiceResponse<String>> removeUsersFromGroup(
      final UserToken userInfo, final long groupId, final GroupRemoveUsersContract usersToRemove) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.REMOVE_USER_FROM_GROUP,
        user ->
            groupPermissionsService.userHasPermissionError(
                ResourceType.GROUP,
                ActionType.REMOVE_USER_FROM_GROUP,
                user.getId(),
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
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                ResourceType.GROUP,
                                                ActionType.REMOVE_USER_FROM_GROUP,
                                                List.of(
                                                    ServiceError.builder()
                                                        .errorCode(ErrorCode.GROUP_DOES_NOT_EXIST)
                                                        .errorMessage("Group does not exist")
                                                        .httpCode(404)
                                                        .build()))));
                              }
                              ArrayList<Uni<ServiceActionResponse<String>>>
                                  individualRemoveUserUnis = new ArrayList<>();
                              usersToRemove
                                  .usersToRemove()
                                  .forEach(
                                      username ->
                                          individualRemoveUserUnis.add(
                                              removeIndividualUser(
                                                  groupId, groupOwnerId, username)));
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
      final long groupId, final long groupOwnerId, final String username) {
    return userService
        .getUserByUsername(username)
        .chain(
            userServiceResponse -> {
              if (userServiceResponse.hasError()) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            ResourceType.GROUP,
                            ActionType.REMOVE_USER_FROM_GROUP,
                            userServiceResponse.getActionResponses().getFirst().getErrors()));
              }

              User userToRemove = userServiceResponse.getActionResponses().getFirst().getData();

              if (userToRemove.getId() == groupOwnerId) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            ResourceType.GROUP,
                            ActionType.REMOVE_USER_FROM_GROUP,
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
                                      ResourceType.GROUP,
                                      ActionType.REMOVE_USER_FROM_GROUP,
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
                                          ResourceType.GROUP,
                                          ActionType.REMOVE_USER_FROM_GROUP,
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
                                        ResourceType.GROUP,
                                        ActionType.REMOVE_USER_FROM_GROUP,
                                        username);
                                  });
                        }
                      });
            });
  }

  public Uni<ServiceResponse<GroupItem>> addItemsToGroup(
      final UserToken userInfo, final long groupId, final GroupAddItemContract itemsToAdd) {
    // Step 1: Check user exists
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.ADD_ITEM_TO_GROUP,
        user ->
            // Step 2: check user is member of group and accepted
            groupRepo
                .userActiveMemberInGroup(user.getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.GROUP,
                                        ActionType.ADD_ITEM_TO_GROUP,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                                .errorMessage(
                                                    "User ("
                                                        + userInfo.getId()
                                                        + ") is not active in group ("
                                                        + groupId
                                                        + "). Check that the group exists and user is active member. ")
                                                .httpCode(400)
                                                .build()))));
                      }

                      // step 3: Check user has permission to add
                      // step 4: iterate over each item and add them individually
                      return groupPermissionsService.userHasPermissionError(
                          ResourceType.GROUP,
                          ActionType.ADD_ITEM_TO_GROUP,
                          user.getId(),
                          groupId,
                          GroupPermissionType.ADD_ITEMS,
                          () -> addEachItemIndividually(itemsToAdd, groupId, user.getId()));
                    }));
  }

  Uni<ServiceResponse<GroupItem>> addEachItemIndividually(
      final GroupAddItemContract itemsToAdd, final long groupId, final long userId) {
    ArrayList<Uni<ServiceActionResponse<GroupItem>>> individualItemAdd = new ArrayList<>();

    itemsToAdd
        .itemsToAdd()
        .forEach(
            itemContract ->
                individualItemAdd.add(individualAddItem(itemContract, groupId, userId)));

    return Uni.combine()
        .all()
        .unis(individualItemAdd)
        .with(toCombine -> Utils.combineServiceActionResponses(toCombine, GroupItem.class));
  }

  Uni<ServiceActionResponse<GroupItem>> individualAddItem(
      final GroupItemContract itemToAdd, final long groupId, final long userId) {
    // Step 1: Check that the item id is not null and < 0
    if (itemToAdd.itemId() < 0) {
      return Uni.createFrom()
          .item(
              new ServiceActionResponse<>(
                  ResourceType.GROUP,
                  ActionType.ADD_ITEM_TO_GROUP,
                  List.of(
                      ServiceError.builder()
                          .errorCode(ErrorCode.INVALID_INPUT_VALUE)
                          .errorMessage("Item id must be 0 or greater.")
                          .httpCode(400)
                          .build())));
    }

    // Step 2: Check that the item type is not null and not UNKNOWN
    if (itemToAdd.itemType() == null || itemToAdd.itemType().equals(GroupItemType.UNKNOWN)) {
      return Uni.createFrom()
          .item(
              new ServiceActionResponse<>(
                  ResourceType.GROUP,
                  ActionType.ADD_ITEM_TO_GROUP,
                  List.of(
                      ServiceError.builder()
                          .errorCode(ErrorCode.INVALID_INPUT_VALUE)
                          .errorMessage("Item type must be present and NOT UNKNOWN.")
                          .httpCode(400)
                          .build())));
    }

    // Step 3: Check item exists
    return groupItemService
        .itemExists(itemToAdd.itemType(), itemToAdd.itemId(), userId)
        .chain(
            itemExist -> {
              if (!itemExist) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            ResourceType.GROUP,
                            ActionType.ADD_ITEM_TO_GROUP,
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
                                      ResourceType.GROUP,
                                      ActionType.ADD_ITEM_TO_GROUP,
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
                                        ResourceType.GROUP,
                                        ActionType.ADD_ITEM_TO_GROUP,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.UNABLE_TO_ADD_ITEM_TO_GROUP)
                                                .errorMessage(
                                                    "Error inserting item to DB. Contact support.")
                                                .httpCode(500)
                                                .build()));
                                  }

                                  return new ServiceActionResponse<>(
                                      ResourceType.GROUP,
                                      ActionType.ADD_ITEM_TO_GROUP,
                                      newGroupItem);
                                });
                      });
            });
  }

  public Uni<ServiceResponse<GroupItem>> removeItemsFromGroup(
      final UserToken userInfo, final long groupId, final GroupRemoveItemContract itemsToRemove) {
    // Step 1: Check user exists
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.REMOVE_ITEM_FROM_GROUP,
        user ->
            groupRepo
                .userActiveMemberInGroup(user.getId(), groupId)
                .chain(
                    isActiveMember -> {
                      if (!isActiveMember) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.GROUP,
                                        ActionType.REMOVE_ITEM_FROM_GROUP,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                                .errorMessage(
                                                    "User ("
                                                        + userInfo.getId()
                                                        + ") is not active in group ("
                                                        + groupId
                                                        + "). Check that the group exists and user is active member. ")
                                                .httpCode(400)
                                                .build()))));
                      }

                      // step 3: Check user has permission to add
                      // step 4: iterate over each item and add them individually
                      return groupPermissionsService.userHasPermissionError(
                          ResourceType.GROUP,
                          ActionType.REMOVE_ITEM_FROM_GROUP,
                          user.getId(),
                          groupId,
                          GroupPermissionType.REMOVE_ITEMS,
                          () -> removeEachItemIndividually(itemsToRemove, groupId));
                    }));
  }

  Uni<ServiceResponse<GroupItem>> removeEachItemIndividually(
      final GroupRemoveItemContract itemsToRemove, final long groupId) {
    ArrayList<Uni<ServiceActionResponse<GroupItem>>> individualItemRemove = new ArrayList<>();

    itemsToRemove
        .itemsToRemove()
        .forEach(
            itemContract -> individualItemRemove.add(individualRemoveItem(itemContract, groupId)));

    return Uni.combine()
        .all()
        .unis(individualItemRemove)
        .with(toCombine -> Utils.combineServiceActionResponses(toCombine, GroupItem.class));
  }

  Uni<ServiceActionResponse<GroupItem>> individualRemoveItem(
      final GroupItemContract itemToRemove, final long groupId) {
    // Step 1: Check that the item id is not null and < 0
    if (itemToRemove.itemId() < 0) {
      return Uni.createFrom()
          .item(
              new ServiceActionResponse<>(
                  ResourceType.GROUP,
                  ActionType.REMOVE_ITEM_FROM_GROUP,
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
                  ResourceType.GROUP,
                  ActionType.REMOVE_ITEM_FROM_GROUP,
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
                    ResourceType.GROUP,
                    ActionType.REMOVE_ITEM_FROM_GROUP,
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
                  ResourceType.GROUP, ActionType.REMOVE_ITEM_FROM_GROUP, returnedGroupItem);
            });
  }
}
