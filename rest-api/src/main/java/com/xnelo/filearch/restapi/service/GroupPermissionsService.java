package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupMemberAllPermissions;
import com.xnelo.filearch.common.model.GroupMemberPermission;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.api.contracts.GroupMemberPermissionModifyContract;
import com.xnelo.filearch.restapi.data.GroupMemberPermissionsRepo;
import com.xnelo.filearch.restapi.data.GroupRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@RequestScoped
public class GroupPermissionsService {
  @Inject UserService userService;
  @Inject GroupRepo groupRepo;
  @Inject GroupService groupService;
  @Inject GroupMemberPermissionsRepo groupMemberPermissionsRepo;

  public Uni<ServiceResponse<List<GroupMemberAllPermissions>>> getAllGroupPermissionByUser(
      final ServiceRequestContext requestContext) {
    Utils.checkGroupInRequest(requestContext);

    return userService
        .checkUserExist(requestContext)
        .chain(context -> groupService.checkUserActiveMember(context))
        .chain(context -> canUserViewPermissions(context, context.getUser().getId(), null))
        .chain(
            context ->
                groupMemberPermissionsRepo.getAllGroupPermissionsByUser(context.getGroupId()))
        .map(
            res -> {
              List<GroupMemberAllPermissions> finalOutput =
                  res.entrySet().stream()
                      .map(
                          (e) ->
                              new GroupMemberAllPermissions(
                                  e.getKey(),
                                  requestContext.getGroupId(),
                                  e.getValue().stream()
                                      .map(GroupMemberPermission::getPermission)
                                      .toList()))
                      .toList();
              return new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
                      finalOutput));
            });
  }

  public Uni<ServiceResponse<List<GroupMemberPermission>>> getUserPermissions(
      final ServiceRequestContext requestContext, final long userToViewId) {
    Utils.checkGroupInRequest(requestContext);

    return userService
        .checkUserExist(requestContext)
        .chain(context -> groupService.checkUserActiveMember(requestContext))
        .chain(context -> canUserViewPermissions(context, context.getUser().getId(), userToViewId))
        .chain(
            context ->
                groupMemberPermissionsRepo.getPermissions(userToViewId, context.getGroupId()))
        .map(
            permissions ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        permissions)));
  }

  Uni<ServiceRequestContext> canUserViewPermissions(
      final ServiceRequestContext context, final long userRequestingView, final Long userToView) {
    Utils.checkGroupInRequest(context);

    if (userToView != null && userRequestingView == userToView) {
      // We can view... don't throw exception
      return Uni.createFrom().item(context);
    }

    return userHasPermission(
            userRequestingView, context.getGroupId(), GroupPermissionType.EDIT_MEMBER_PERMISSIONS)
        .map(
            hasPermission -> {
              if (!hasPermission) {
                throw new ServiceResponseException(
                    context,
                    ErrorCode.PERMISSION_NOT_GRANTED,
                    "You do not have permission to view user permission in this group("
                        + context.getGroupId()
                        + ")",
                    403);
              }

              return context;
            });
  }

  public Uni<ServiceResponse<GroupMemberPermission>> modifyPermissions(
      final ServiceRequestContext requestContext,
      final long groupId,
      final List<GroupMemberPermissionModifyContract> permissionModifications) {
    return userService
        .checkUserExist(requestContext)
        .chain(context -> groupService.checkUserActiveMember(context))
        .chain(context -> userHasPermissionV2(context, GroupPermissionType.EDIT_MEMBER_PERMISSIONS))
        .chain(
            context -> {
              ArrayList<Uni<ServiceActionResponse<GroupMemberPermission>>>
                  individualPermissionModifications = new ArrayList<>();
              for (GroupMemberPermissionModifyContract permissionModifyContract :
                  permissionModifications) {
                individualPermissionModifications.add(
                    individualModifyPermission(context, groupId, permissionModifyContract));
              }

              return Uni.combine()
                  .all()
                  .unis(individualPermissionModifications)
                  .with(
                      toCombine ->
                          Utils.combineServiceActionResponses(
                              toCombine, GroupMemberPermission.class));
            });
  }

  /**
   * Check if a specific user has a specific permission. This will throw a ServiceResponseException
   * exception if the user does NOT have the required permission.
   *
   * @param requestContext The context of the overall request.
   * @param groupId The group the user needs permissions on.
   * @param permissionNeeded The specific permission needed for the action.
   * @param hasPermissionAction The action to execute if the permission exists and is valid.
   * @return A Uni with the ServiceResponse in it.
   * @param <T> The specific resource type object.
   */
  @Deprecated
  public <T> Uni<ServiceResponse<T>> userHasPermissionError(
      final ServiceRequestContext requestContext,
      final long groupId,
      final GroupPermissionType permissionNeeded,
      final Supplier<Uni<ServiceResponse<T>>> hasPermissionAction) {
    return userHasPermission(requestContext.getUser().getId(), groupId, permissionNeeded)
        .chain(
            hasPermission -> {
              if (!hasPermission) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.PERMISSION_NOT_GRANTED,
                    "User("
                        + requestContext.getUser().getId()
                        + ") does not have permission("
                        + permissionNeeded
                        + ") on group("
                        + groupId
                        + ").",
                    403);
              }

              return hasPermissionAction.get();
            });
  }

  /**
   * Check if a specific user has a specific permission.
   *
   * @param userId The user to check permissions on.
   * @param groupId The group the user has permissions on.
   * @param permissionNeeded The specific permission needed.
   * @return If the user has the specific permission on the group then true is returned.
   */
  Uni<Boolean> userHasPermission(
      final long userId, final long groupId, final GroupPermissionType permissionNeeded) {
    return groupRepo
        .getGroupById(userId, groupId)
        .chain(
            group -> {
              if (group != null) { // we are owner of group
                return Uni.createFrom().item(true);
              }

              // We do NOT own the group... see if we have ADMIN or permissionNeeded
              return groupMemberPermissionsRepo
                  .getPermissions(userId, groupId)
                  .map(
                      permissions ->
                          permissions.stream()
                              .anyMatch(
                                  permission ->
                                      permission.getPermission() == GroupPermissionType.ADMIN
                                          || permission.getPermission() == permissionNeeded));
            });
  }

  Uni<ServiceActionResponse<GroupMemberPermission>> individualModifyPermission(
      final ServiceRequestContext requestContext,
      final long groupId,
      GroupMemberPermissionModifyContract permissionModifyContract) {
    return switch (permissionModifyContract.getModifyAction()) {
      case ADD ->
          addIndividualPermission(
              requestContext,
              permissionModifyContract.getUserId(),
              groupId,
              permissionModifyContract.getPermission());
      case REMOVE ->
          removeIndividualPermission(
              requestContext,
              permissionModifyContract.getUserId(),
              groupId,
              permissionModifyContract.getPermission());
    };
  }

  Uni<ServiceActionResponse<GroupMemberPermission>> addIndividualPermission(
      final ServiceRequestContext requestContext,
      final long userId,
      final long groupId,
      final GroupPermissionType permissionToAdd) {
    return groupMemberPermissionsRepo
        .permissionExists(userId, groupId, permissionToAdd)
        .invoke(
            hasPermission -> {
              if (!hasPermission) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.PERMISSION_ALREADY_GRANTED,
                    "User already has permission, userId="
                        + userId
                        + ", groupId="
                        + groupId
                        + ", permission="
                        + permissionToAdd,
                    400);
              }
            })
        .chain(
            _ignore -> groupMemberPermissionsRepo.addPermission(userId, groupId, permissionToAdd))
        .map(
            addData -> {
              if (addData == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.ERROR_CREATING_PERMISSION,
                    "Error giving userId="
                        + userId
                        + ", groupId="
                        + groupId
                        + " permission="
                        + permissionToAdd,
                    500);
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(), requestContext.getActionType(), addData);
            })
        .onFailure(ServiceResponseException.class)
        .recoverWithItem(ServiceResponseException::toServiceActionResponse);
  }

  Uni<ServiceActionResponse<GroupMemberPermission>> removeIndividualPermission(
      final ServiceRequestContext requestContext,
      final long userId,
      final long groupId,
      final GroupPermissionType permissionToRemove) {
    return groupMemberPermissionsRepo
        .removePermission(userId, groupId, permissionToRemove)
        .map(
            success -> {
              if (!success) {
                return new ServiceActionResponse<>(
                    requestContext.getResourceType(),
                    requestContext.getActionType(),
                    List.of(
                        ServiceError.builder()
                            .errorCode(ErrorCode.UNABLE_TO_DELETE_GROUP_USER_PERMISSIONS)
                            .errorMessage(
                                "Error while deleting permission from userid="
                                    + userId
                                    + ", groupId="
                                    + groupId
                                    + ", permission="
                                    + permissionToRemove)
                            .httpCode(500)
                            .build()));
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(),
                  requestContext.getActionType(),
                  new GroupMemberPermission(userId, groupId, permissionToRemove));
            });
  }

  Uni<ServiceRequestContext> userHasPermissionV2(
      ServiceRequestContext requestContext, final GroupPermissionType permission) {
    Utils.checkUserInRequest(requestContext);
    Utils.checkGroupInRequest(requestContext);
    return userHasPermission(
            requestContext.getUser().getId(), requestContext.getGroupId(), permission)
        .map(
            res -> {
              if (!res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.PERMISSION_NOT_GRANTED,
                    "User does not have correct permissions.",
                    403);
              }

              return requestContext;
            });
  }
}
