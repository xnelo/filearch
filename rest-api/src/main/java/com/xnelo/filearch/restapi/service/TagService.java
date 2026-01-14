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
}
