package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.TagContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.data.TagRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class TagService {
  @Inject UserService userService;
  @Inject TagRepo tagRepo;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  public Uni<ServiceResponse<PaginatedResponse<Tag>>> getAllTags(
      final UserToken userInfo,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    if (after != null && after < 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.TAG,
                      ActionType.GET,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_AFTER_VALUE)
                              .errorMessage("After value must be greater than 0.")
                              .httpCode(400)
                              .build()))));
    }

    if (limit != null && limit <= 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.GET,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_RESPONSE_LIMIT)
                              .errorMessage(
                                  "A return limit of '"
                                      + limit
                                      + "' is invalid. Must be greater than 0")
                              .httpCode(400)
                              .build()))));
    }

    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              User user = userResponse.getActionResponses().getFirst().getData();
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FILE,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage("User does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return tagRepo
                  .getAll(user.getId(), after, limit, sortDirection)
                  .map(
                      paginatedTags ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.TAG,
                                  ActionType.GET,
                                  paginationMapper.toPaginatedResponse(paginatedTags))));
            });
  }

  public Uni<ServiceResponse<Tag>> getTagById(final long tagId, final long userId) {
    return tagRepo
        .getTagById(tagId, userId)
        .map(
            tag -> {
              if (tag == null) {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        ResourceType.TAG,
                        ActionType.GET,
                        List.of(
                            ServiceError.builder()
                                .errorCode(ErrorCode.TAG_DOES_NOT_EXIST)
                                .errorMessage("Tag does not exist.")
                                .httpCode(404)
                                .build())));
              }

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(ResourceType.TAG, ActionType.GET, tag));
            });
  }

  public Uni<ServiceResponse<Tag>> getTagById(final long tagId, final UserToken userToken) {
    return userService.checkUserExist(
        userToken, ResourceType.TAG, ActionType.GET, user -> getTagById(tagId, user.getId()));
  }

  public Uni<ServiceResponse<Tag>> createNewTag(
      final TagContract newTag, final UserToken userToken) {
    return userService.checkUserExist(
        userToken,
        ResourceType.TAG,
        ActionType.CREATE,
        user ->
            tagRepo
                .tagNameExists(user.getId(), newTag.getTagName())
                .chain(
                    nameExists -> {
                      if (nameExists) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.TAG,
                                        ActionType.CREATE,
                                        List.of(
                                            ServiceError.builder()
                                                .httpCode(400)
                                                .errorCode(ErrorCode.TAG_WITH_NAME_ALREADY_EXISTS)
                                                .errorMessage(
                                                    "A Tag with the name '"
                                                        + newTag.getTagName()
                                                        + "' already exists.")
                                                .build()))));
                      }

                      // All checks passed... insert new tag
                      return tagRepo
                          .createTag(user.getId(), newTag.getTagName())
                          .map(
                              newlyCreatedTag ->
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          ResourceType.TAG, ActionType.CREATE, newlyCreatedTag)));
                    }));
  }

  public Uni<ServiceResponse<Tag>> updateTag(
      final long tagId, final UserToken userInfo, final TagContract tagData) {
    if (tagData.getId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.TAG,
                      ActionType.UPDATE,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.TAG_ID_CANNOT_BE_UPDATED)
                              .errorMessage("Tag id cannot be updated.")
                              .httpCode(400)
                              .build()))));
    } else if (tagData.getOwnerId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.TAG,
                      ActionType.UPDATE,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.TAG_OWNER_CANNOT_BE_UPDATED)
                              .errorMessage("Tag owner cannot be updated.")
                              .httpCode(400)
                              .build()))));
    }

    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              if (userResponse.hasError()) {
                return updateErrorAndPassThrough(userResponse);
              }
              User user = userResponse.getActionResponses().getFirst().getData();
              return tagRepo
                  .getTagById(tagId, user.getId())
                  .chain(
                      existingTag -> {
                        if (existingTag == null) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          ResourceType.TAG,
                                          ActionType.UPDATE,
                                          List.of(
                                              ServiceError.builder()
                                                  .errorCode(ErrorCode.TAG_DOES_NOT_EXIST)
                                                  .errorMessage("Tag does not exist.")
                                                  .httpCode(404)
                                                  .build()))));
                        }
                        if (tagData.getTagName() == null) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          ResourceType.TAG,
                                          ActionType.UPDATE,
                                          List.of(
                                              ServiceError.builder()
                                                  .errorCode(ErrorCode.TAG_NO_UPDATES_EXECUTED)
                                                  .errorMessage(
                                                      "No updates to execute on this tag.")
                                                  .httpCode(400)
                                                  .build()))));
                        }
                        // check if tag name exists for user
                        return tagRepo
                            .tagNameExists(user.getId(), tagData.getTagName())
                            .chain(
                                nameExists -> {
                                  if (nameExists) {
                                    return Uni.createFrom()
                                        .item(
                                            new ServiceResponse<>(
                                                new ServiceActionResponse<>(
                                                    ResourceType.TAG,
                                                    ActionType.UPDATE,
                                                    List.of(
                                                        ServiceError.builder()
                                                            .errorCode(
                                                                ErrorCode
                                                                    .TAG_WITH_NAME_ALREADY_EXISTS)
                                                            .errorMessage(
                                                                "A tag with name '"
                                                                    + tagData.getTagName()
                                                                    + "' already exists.")
                                                            .httpCode(400)
                                                            .build()))));
                                  }

                                  return tagRepo
                                      .updateName(tagId, user.getId(), tagData.getTagName())
                                      .map(
                                          updatedTag ->
                                              new ServiceResponse<>(
                                                  new ServiceActionResponse<>(
                                                      ResourceType.TAG,
                                                      ActionType.UPDATE,
                                                      updatedTag)));
                                });
                      });
            });
  }

  private Uni<ServiceResponse<Tag>> updateErrorAndPassThrough(ServiceResponse<?> response) {
    ArrayList<ServiceActionResponse<Tag>> actionResponses = new ArrayList<>();
    for (ServiceActionResponse<?> actionResponse : response.getActionResponses()) {
      actionResponses.add(
          new ServiceActionResponse<>(
              ResourceType.TAG, actionResponse.getActionType(), actionResponse.getErrors()));
    }
    return Uni.createFrom().item(new ServiceResponse<>(actionResponses));
  }
}
