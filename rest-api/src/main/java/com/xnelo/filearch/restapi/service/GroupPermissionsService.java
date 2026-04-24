package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupMemberPermission;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.GroupMemberPermissionModifyContract;
import com.xnelo.filearch.restapi.data.GroupMemberPermissionsRepo;
import com.xnelo.filearch.restapi.data.GroupRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@RequestScoped
public class GroupPermissionsService {
  @Inject UserService userService;
  @Inject GroupRepo groupRepo;
  @Inject GroupMemberPermissionsRepo groupMemberPermissionsRepo;

  public Uni<ServiceResponse<List<GroupMemberPermission>>> getUserPermissions(
      final UserToken userInfo, final long userToViewId, final long groupId) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.GET_GROUP_PERMISSIONS,
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
                                        ActionType.GET_GROUP_PERMISSIONS,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.GROUP_DOES_NOT_EXIST)
                                                .errorMessage(
                                                    "Group ("
                                                        + groupId
                                                        + ") does not exits or user is not a member of group.")
                                                .httpCode(404)
                                                .build()))));
                      }

                      return canUserViewPermissions(user.getId(), userToViewId, groupId)
                          .chain(
                              canView -> {
                                if (!canView) {
                                  return Uni.createFrom()
                                      .item(
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  ResourceType.GROUP,
                                                  ActionType.GET_GROUP_PERMISSIONS,
                                                  List.of(
                                                      ServiceError.builder()
                                                          .errorCode(
                                                              ErrorCode.PERMISSION_NOT_GRANTED)
                                                          .errorMessage(
                                                              "You do not have permission to view other users permissions.")
                                                          .httpCode(403)
                                                          .build()))));
                                }

                                return groupMemberPermissionsRepo
                                    .getPermissions(userToViewId, groupId)
                                    .map(
                                        permissions ->
                                            new ServiceResponse<>(
                                                new ServiceActionResponse<>(
                                                    ResourceType.GROUP,
                                                    ActionType.GET_GROUP_PERMISSIONS,
                                                    permissions)));
                              });
                    }));
  }

  Uni<Boolean> canUserViewPermissions(
      final long userRequestingView, final long userToView, final long groupId) {
    if (userRequestingView == userToView) {
      return Uni.createFrom().item(true);
    }

    return userHasPermission(
        userRequestingView, groupId, GroupPermissionType.EDIT_MEMBER_PERMISSIONS);
  }

  public Uni<ServiceResponse<GroupMemberPermission>> modifyPermissions(
      final UserToken userInfo,
      final long groupId,
      final List<GroupMemberPermissionModifyContract> permissionModifications) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.GROUP,
        ActionType.MODIFY_GROUP_PERMISSIONS,
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
                                        ActionType.MODIFY_GROUP_PERMISSIONS,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                                .errorMessage(
                                                    "User is not an active member of the group")
                                                .httpCode(403)
                                                .build()))));
                      }

                      return userHasPermission(
                              user.getId(), groupId, GroupPermissionType.EDIT_MEMBER_PERMISSIONS)
                          .chain(
                              canModify -> {
                                if (!canModify) {
                                  return Uni.createFrom()
                                      .item(
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  ResourceType.GROUP,
                                                  ActionType.MODIFY_GROUP_PERMISSIONS,
                                                  List.of(
                                                      ServiceError.builder()
                                                          .errorCode(
                                                              ErrorCode.PERMISSION_NOT_GRANTED)
                                                          .errorMessage(
                                                              "User does not have permission to modify user permissions.")
                                                          .httpCode(403)
                                                          .build()))));
                                }

                                ArrayList<Uni<ServiceActionResponse<GroupMemberPermission>>>
                                    individualPermissionModifications = new ArrayList<>();
                                for (GroupMemberPermissionModifyContract permissionModifyContract :
                                    permissionModifications) {
                                  individualPermissionModifications.add(
                                      individualModifyPermission(
                                          groupId, permissionModifyContract));
                                }

                                return Uni.combine()
                                    .all()
                                    .unis(individualPermissionModifications)
                                    .with(
                                        toCombine ->
                                            Utils.combineServiceActionResponses(
                                                toCombine, GroupMemberPermission.class));
                              });
                    }));
  }

  Uni<Boolean> userHasPermission(
      final long userId, final long groupId, final GroupPermissionType permissionNeeded) {
    return groupRepo
        .getGroupById(userId, groupId)
        .chain(
            group -> {
              if (group != null) { // we are owner of group
                return Uni.createFrom().item(true);
              }

              // We do NOT own the group... see if we have ADMIN or EDIT_MEMBER_PERMISSIONS
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
      final long groupId, GroupMemberPermissionModifyContract permissionModifyContract) {
    return switch (permissionModifyContract.getModifyAction()) {
      case ADD ->
          addIndividualPermission(
              permissionModifyContract.getUserId(),
              groupId,
              permissionModifyContract.getPermission());
      case REMOVE ->
          removeIndividualPermission(
              permissionModifyContract.getUserId(),
              groupId,
              permissionModifyContract.getPermission());
    };
  }

  Uni<ServiceActionResponse<GroupMemberPermission>> addIndividualPermission(
      final long userId, final long groupId, final GroupPermissionType permissionToAdd) {
    return groupMemberPermissionsRepo
        .permissionExists(userId, groupId, permissionToAdd)
        .chain(
            hasPermission -> {
              if (hasPermission) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            ResourceType.GROUP,
                            ActionType.MODIFY_GROUP_PERMISSIONS,
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.PERMISSION_ALREADY_GRANTED)
                                    .errorMessage(
                                        "User already has permission, userId="
                                            + userId
                                            + ", groupId="
                                            + groupId
                                            + ", permission="
                                            + permissionToAdd)
                                    .httpCode(400)
                                    .build())));
              }

              return groupMemberPermissionsRepo
                  .addPermission(userId, groupId, permissionToAdd)
                  .map(
                      addData -> {
                        if (addData == null) {
                          return new ServiceActionResponse<>(
                              ResourceType.GROUP,
                              ActionType.MODIFY_GROUP_PERMISSIONS,
                              List.of(
                                  ServiceError.builder()
                                      .errorCode(ErrorCode.ERROR_CREATING_PERMISSION)
                                      .errorMessage(
                                          "Error giving userId="
                                              + userId
                                              + ", groupId="
                                              + groupId
                                              + " permission="
                                              + permissionToAdd)
                                      .httpCode(500)
                                      .build()));
                        }

                        return new ServiceActionResponse<>(
                            ResourceType.GROUP, ActionType.MODIFY_GROUP_PERMISSIONS, addData);
                      });
            });
  }

  Uni<ServiceActionResponse<GroupMemberPermission>> removeIndividualPermission(
      final long userId, final long groupId, final GroupPermissionType permissionToRemove) {
    return groupMemberPermissionsRepo
        .removePermission(userId, groupId, permissionToRemove)
        .map(
            success -> {
              if (!success) {
                return new ServiceActionResponse<>(
                    ResourceType.GROUP,
                    ActionType.MODIFY_GROUP_PERMISSIONS,
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
                  ResourceType.GROUP,
                  ActionType.MODIFY_GROUP_PERMISSIONS,
                  new GroupMemberPermission(userId, groupId, permissionToRemove));
            });
  }
}
