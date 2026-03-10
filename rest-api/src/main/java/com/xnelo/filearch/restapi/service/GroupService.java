package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.Group;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.GroupCreateContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.data.GroupRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@RequestScoped
public class GroupService {
  @Inject UserService userService;
  @Inject GroupRepo groupRepo;
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
                                              .map(
                                                  res -> {
                                                    if (res == false) {
                                                      return new ServiceResponse<>(
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
                    }));
  }
}
