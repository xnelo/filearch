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
import com.xnelo.filearch.restapi.data.GroupMemberPermissionsRepo;
import com.xnelo.filearch.restapi.data.GroupRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
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

    return groupRepo
        .getGroupById(userRequestingView, groupId)
        .chain(
            groupInfo -> {
              // This means we own the group we can see member permissions
              if (groupInfo != null) {
                return Uni.createFrom().item(true);
              }

              // We do NOT own the group see if we have ADMIN or EDIT_MEMBER_PREMISSIONS permission
              return groupMemberPermissionsRepo
                  .getPermissions(userRequestingView, groupId)
                  .map(
                      permissions ->
                          permissions.stream()
                              .anyMatch(
                                  p ->
                                      p.getPermission()
                                              == GroupPermissionType.EDIT_MEMBER_PERMISSIONS
                                          || p.getPermission() == GroupPermissionType.ADMIN));
            });
  }
}
