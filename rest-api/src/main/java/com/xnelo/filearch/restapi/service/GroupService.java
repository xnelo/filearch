package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.RepoException;
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

    return userService
        .checkUserExist(requestContext)
        .chain(
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

    return userService
        .checkUserExist(requestContext)
        .chain(
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
    return userService
        .checkUserExist(requestContext)
        .chain(
            context2 ->
                groupRepo.groupNameExists(context2.getUser().getId(), newGroup.getGroupName()))
        .invoke(
            groupExists -> {
              if (groupExists) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.GROUP_WITH_NAME_ALREADY_EXISTS,
                    "A group with the name " + newGroup.getGroupName() + " already exists.",
                    400);
              }
            })
        .chain(_ignore -> createGroupAndAddUser(requestContext, newGroup.getGroupName()));
  }

  Uni<ServiceResponse<Group>> createGroupAndAddUser(
      final ServiceRequestContext requestContext, final String groupName) {
    return groupRepo
        .createGroup(requestContext.getUser().getId(), groupName)
        .chain(
            createResponse -> {
              if (createResponse == null) {
                throw new ServiceResponseException(
                    requestContext, ErrorCode.UNABLE_TO_CREATE_GROUP, "Error creating group.", 500);
              }

              return groupRepo
                  .addUserToGroup(requestContext.getUser().getId(), createResponse.getId(), true)
                  .map(
                      memberAdded -> {
                        if (!memberAdded) {
                          throw new ServiceResponseException(
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

  public Uni<ServiceResponse<Group>> deleteGroup(final ServiceRequestContext requestContext) {
    Utils.checkGroupInRequest(requestContext);

    return userService
        .checkUserExist(requestContext)
        .chain(this::checkGroupExists)
        .chain(context -> groupRepo.deleteGroup(context.getUser().getId(), context.getGroupId()))
        .chain(_ignored -> groupRepo.deleteAllUsersFromGroup(requestContext.getGroupId()))
        .chain(_ignored -> groupRepo.deleteAllItemsFromGroup(requestContext.getGroupId()))
        .chain(
            _ignored ->
                groupMemberPermissionsRepo.deleteAllGroupPermissions(requestContext.getGroupId()))
        .map(
            _ignored -> {
              Group groupInfo = requestContext.getDataAs(GROUP_EXIST_KEY, Group.class);

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(), requestContext.getActionType(), groupInfo));
            })
        .onFailure(RepoException.class)
        .transform(
            ex ->
                new ServiceResponseException(
                    requestContext, ex.getErrorCode(), ex.getMessage(), 500));
  }

  public Uni<ServiceResponse<String>> addUsersToGroup(
      final ServiceRequestContext requestContext, final GroupAddUsersContract usersToAdd) {
    return userService
        .checkUserExist(requestContext)
        .chain(this::checkGroupExists)
        .chain(
            context ->
                groupPermissionsService.userHasPermissionV2(
                    context, GroupPermissionType.ADD_MEMBERS))
        .chain(
            context -> {
              // if this is being executed then the group exists
              ArrayList<Uni<ServiceActionResponse<String>>> individualUserAdds = new ArrayList<>();
              usersToAdd
                  .usersToAdd()
                  .forEach(
                      individualUser ->
                          individualUserAdds.add(addSingleUserToGroup(context, individualUser)));
              return Uni.combine()
                  .all()
                  .unis(individualUserAdds)
                  .with(toCombine -> Utils.combineServiceActionResponses(toCombine, String.class));
            });
  }

  Uni<ServiceActionResponse<String>> addSingleUserToGroup(
      final ServiceRequestContext requestContext, final String username) {
    Utils.checkGroupInRequest(requestContext);

    final String USER_TO_ADD_KEY = "USER_TO_ADD__USER";

    return userService
        .getUserByUsername(requestContext, username)
        .invoke(userToAddData -> requestContext.setData(USER_TO_ADD_KEY, userToAddData))
        .chain(
            userToAddData ->
                groupRepo.userInGroup(userToAddData.getId(), requestContext.getGroupId()))
        .invoke(
            userInGroup -> {
              if (userInGroup != null && userInGroup) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.UNABLE_TO_ADD_USER_TO_GROUP,
                    "User ("
                        + username
                        + ") is already part of group ("
                        + requestContext.getGroupId()
                        + ")",
                    400);
              }
            })
        .chain(
            _ignored -> {
              User userToAddData = requestContext.getDataAs(USER_TO_ADD_KEY, User.class);
              return groupRepo.addUserToGroup(
                  userToAddData.getId(), requestContext.getGroupId(), false);
            })
        .map(
            successfullyAdded -> {
              if (successfullyAdded) {
                return new ServiceActionResponse<>(
                    requestContext.getResourceType(), requestContext.getActionType(), username);
              } else {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.UNABLE_TO_ADD_USER_TO_GROUP,
                    "Unable to add '" + username + "' to group.",
                    500);
              }
            })
        .onFailure(ServiceResponseException.class)
        .recoverWithItem(ServiceResponseException::toServiceActionResponse);
  }

  public Uni<ServiceResponse<Boolean>> acceptGroupInvitation(
      final ServiceRequestContext requestContext, final long groupId) {
    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> groupRepo.acceptGroupInvite(context2.getUser().getId(), groupId))
        .map(
            acceptSuccess -> {
              if (!acceptSuccess) {
                return Utils.createServiceErrorResponse(
                    requestContext,
                    ErrorCode.UNABLE_TO_ACCEPT_GROUP_INVITE,
                    "User is has not been invited to group.",
                    400);
              } else {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        true // This will always be true since we have an error message for false
                        ));
              }
            });
  }

  public Uni<ServiceResponse<String>> removeUsersFromGroup(
      final ServiceRequestContext requestContext, final GroupRemoveUsersContract usersToRemove) {
    Utils.checkGroupInRequest(requestContext);

    return userService
        .checkUserExist(requestContext)
        .chain(
            context2 ->
                groupPermissionsService.userHasPermissionV2(
                    context2, GroupPermissionType.REMOVE_MEMBERS))
        .chain(
            context ->
                groupRepo.getOwnerOfGroup(
                    context.getGroupId())) // if we are here then the group exists, and we have
        // permissions in it.
        .chain(
            groupOwnerId -> {
              if (groupOwnerId == null) {
                throw new ServiceResponseException(
                    requestContext, ErrorCode.GROUP_DOES_NOT_EXIST, "Group does not exist", 404);
              }

              ArrayList<Uni<ServiceActionResponse<String>>> individualRemoveUserUnis =
                  new ArrayList<>();
              usersToRemove
                  .usersToRemove()
                  .forEach(
                      username ->
                          individualRemoveUserUnis.add(
                              removeIndividualUser(requestContext, groupOwnerId, username)));
              return Uni.combine()
                  .all()
                  .unis(individualRemoveUserUnis)
                  .with(toCombine -> Utils.combineServiceActionResponses(toCombine, String.class));
            });
  }

  void checkIfOwner(
      final ServiceRequestContext requestContext,
      final User userToRemove,
      final long groupOwnerId) {
    if (userToRemove.getId() == groupOwnerId) {
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.UNABLE_TO_REMOVE_USER_FROM_GROUP,
          "Cannot remove owner of group from group.",
          400);
    }
  }

  Uni<ServiceActionResponse<String>> removeIndividualUser(
      final ServiceRequestContext requestContext, final long groupOwnerId, final String username) {
    Utils.checkGroupInRequest(requestContext);

    final String USER_TO_REMOVE_KEY = "USER_TO_REMOVE__USER";

    return userService
        .getUserByUsername(requestContext, username)
        .invoke(userToRemove -> checkIfOwner(requestContext, userToRemove, groupOwnerId))
        .invoke(userToRemove -> requestContext.setData(USER_TO_REMOVE_KEY, userToRemove))
        .chain(
            userToRemove ->
                groupRepo.removeUserFromGroup(userToRemove.getId(), requestContext.getGroupId()))
        .invoke(
            removeUserSuccess -> {
              if (!removeUserSuccess) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.UNABLE_TO_REMOVE_USER_FROM_GROUP,
                    "Error while removing user("
                        + username
                        + ") from group ("
                        + requestContext.getGroupId()
                        + ").",
                    500);
              }
            })
        .chain(
            _ignored -> {
              User userToRemoveData = requestContext.getDataAs(USER_TO_REMOVE_KEY, User.class);
              return groupMemberPermissionsRepo.deleteUserPermissionsFromGroup(
                  userToRemoveData.getId(), requestContext.getGroupId());
            })
        .map(
            deletePermissionsSuccess -> {
              if (!deletePermissionsSuccess) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.UNABLE_TO_DELETE_GROUP_USER_PERMISSIONS,
                    "Unable to remove user permissions from group.",
                    500);
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(), requestContext.getActionType(), username);
            })
        .onFailure(ServiceResponseException.class)
        .recoverWithItem(ServiceResponseException::toServiceActionResponse);
  }

  public Uni<ServiceResponse<GroupItem>> addItemsToGroup(
      final ServiceRequestContext requestContext, final GroupAddItemContract itemsToAdd) {
    Utils.checkGroupInRequest(requestContext);

    return userService
        .checkUserExist(requestContext)
        .chain(this::checkUserActiveMember)
        .chain(
            context ->
                groupPermissionsService.userHasPermissionV2(context, GroupPermissionType.ADD_ITEMS))
        .chain(context -> addEachItemIndividually(context, itemsToAdd));
  }

  Uni<ServiceResponse<GroupItem>> addEachItemIndividually(
      final ServiceRequestContext requestContext, final GroupAddItemContract itemsToAdd) {
    ArrayList<Uni<ServiceActionResponse<GroupItem>>> individualItemAdd = new ArrayList<>();

    itemsToAdd
        .itemsToAdd()
        .forEach(
            itemContract -> individualItemAdd.add(individualAddItem(requestContext, itemContract)));

    return Uni.combine()
        .all()
        .unis(individualItemAdd)
        .with(toCombine -> Utils.combineServiceActionResponses(toCombine, GroupItem.class));
  }

  Uni<ServiceActionResponse<GroupItem>> individualAddItem(
      final ServiceRequestContext requestContext, final GroupItemContract itemToAdd) {
    Utils.checkGroupInRequest(requestContext);

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

    return groupItemService
        .checkItemExistsThrowError(requestContext, itemToAdd.itemType(), itemToAdd.itemId())
        .chain(
            context ->
                groupItemService.checkItemNotInGroup(
                    context, itemToAdd.itemId(), itemToAdd.itemType()))
        .chain(
            context ->
                groupItemsRepo.addItemToGroup(
                    itemToAdd.itemId(), itemToAdd.itemType(), context.getGroupId()))
        .map(
            newGroupItem -> {
              if (newGroupItem == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.UNABLE_TO_ADD_ITEM_TO_GROUP,
                    "Error inserting item to DB. Contact support.",
                    500);
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(), requestContext.getActionType(), newGroupItem);
            })
        .onFailure(ServiceResponseException.class)
        .recoverWithItem(ServiceResponseException::toServiceActionResponse);
  }

  public Uni<ServiceResponse<GroupItem>> removeItemsFromGroup(
      final ServiceRequestContext requestContext, final GroupRemoveItemContract itemsToRemove) {
    // Step 1: Check user exists
    return userService
        .checkUserExist(requestContext)
        .chain(this::checkUserActiveMember)
        .chain(
            context ->
                groupPermissionsService.userHasPermissionV2(
                    context, GroupPermissionType.REMOVE_ITEMS))
        .chain(context -> removeEachItemIndividually(context, itemsToRemove));
  }

  Uni<ServiceResponse<GroupItem>> removeEachItemIndividually(
      final ServiceRequestContext requestContext, final GroupRemoveItemContract itemsToRemove) {
    ArrayList<Uni<ServiceActionResponse<GroupItem>>> individualItemRemove = new ArrayList<>();

    itemsToRemove
        .itemsToRemove()
        .forEach(
            itemContract ->
                individualItemRemove.add(individualRemoveItem(requestContext, itemContract)));

    return Uni.combine()
        .all()
        .unis(individualItemRemove)
        .with(toCombine -> Utils.combineServiceActionResponses(toCombine, GroupItem.class));
  }

  Uni<ServiceActionResponse<GroupItem>> individualRemoveItem(
      final ServiceRequestContext requestContext, final GroupItemContract itemToRemove) {
    Utils.checkGroupInRequest(requestContext);

    // Step 1: Check that the item id is not null and < 0
    if (itemToRemove.itemId() < 0) {
      return Uni.createFrom()
          .item(
              Utils.createServiceActionErrorResponse(
                  requestContext,
                  ErrorCode.INVALID_INPUT_VALUE,
                  "Item id must be 0 or greater.",
                  400));
    }

    // Step 2: Check that the item type is not null and not UNKNOWN
    if (itemToRemove.itemType() == null || itemToRemove.itemType().equals(GroupItemType.UNKNOWN)) {
      return Uni.createFrom()
          .item(
              Utils.createServiceActionErrorResponse(
                  requestContext,
                  ErrorCode.INVALID_INPUT_VALUE,
                  "Item type must be present and NOT UNKNOWN.",
                  400));
    }

    return groupItemsRepo
        .removeItemFromGroup(
            itemToRemove.itemId(), itemToRemove.itemType(), requestContext.getGroupId())
        .map(
            returnedGroupItem -> {
              if (returnedGroupItem == null) {
                return Utils.createServiceActionErrorResponse(
                    requestContext,
                    ErrorCode.UNABLE_TO_REMOVE_ITEM_FROM_GROUP,
                    "Unable to remove item("
                        + itemToRemove.itemId()
                        + ") of type("
                        + itemToRemove.itemType()
                        + ") from group ("
                        + requestContext.getGroupId()
                        + ").",
                    500);
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(),
                  requestContext.getActionType(),
                  returnedGroupItem);
            });
  }

  public Uni<ServiceResponse<List<GroupMember>>> getUsersInGroup(
      final ServiceRequestContext requestContext) {
    Utils.checkGroupInRequest(requestContext);

    return userService
        .checkUserExist(requestContext)
        .chain(this::checkUserActiveMember)
        .chain(context -> groupRepo.getUsersInGroup(context.getGroupId()))
        .map(
            groupMembersList ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        groupMembersList)));
  }

  public Uni<ServiceResponse<PaginatedResponse<GroupFile>>> getFilesInGroup(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    Utils.checkGroupInRequest(requestContext);
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService
        .checkUserExist(requestContext)
        .chain(this::checkUserActiveMember)
        .chain(context -> groupRepo.getFilesInGroup(context.getGroupId(), paginationParameters))
        .map(
            data ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        paginationMapper.toPaginatedResponse(data))));
  }

  Uni<ServiceRequestContext> checkGroupExists(ServiceRequestContext requestContext) {
    Utils.checkUserInRequest(requestContext);
    Utils.checkGroupInRequest(requestContext);

    return groupRepo
        .getGroupById(requestContext.getUser().getId(), requestContext.getGroupId())
        .map(
            group -> {
              if (group == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.GROUP_DOES_NOT_EXIST,
                    "Group (" + requestContext.getGroupId() + ") does not exist.",
                    404);
              }

              requestContext.setData(GROUP_EXIST_KEY, group);

              return requestContext;
            });
  }

  public Uni<ServiceRequestContext> checkUserActiveMember(
      ServiceRequestContext requestContext, final long groupId) {
    Utils.checkUserInRequest(requestContext);

    return internalCheckUserActiveMember(requestContext, groupId);
  }

  public Uni<ServiceRequestContext> checkUserActiveMember(ServiceRequestContext requestContext) {
    Utils.checkUserInRequest(requestContext);
    Utils.checkGroupInRequest(requestContext);

    return internalCheckUserActiveMember(requestContext, requestContext.getGroupId());
  }

  private Uni<ServiceRequestContext> internalCheckUserActiveMember(
      final ServiceRequestContext requestContext, final Long groupId) {
    return groupRepo
        .userActiveMemberInGroup(requestContext.getUser().getId(), groupId)
        .map(
            res -> {
              if (!res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.USER_NOT_ACTIVE,
                    "User is not an active member of group(" + groupId + ").",
                    400);
              }

              return requestContext;
            });
  }
}
