package com.xnelo.filearch.restapi.service.tag;

import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.api.contracts.TagContract;
import com.xnelo.filearch.restapi.api.contracts.TagShareBulkContract;
import com.xnelo.filearch.restapi.api.contracts.TagShareContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.data.FileTagsRepo;
import com.xnelo.filearch.restapi.data.GroupRepo;
import com.xnelo.filearch.restapi.data.SharedTagsRepo;
import com.xnelo.filearch.restapi.data.TagRepo;
import com.xnelo.filearch.restapi.service.FileService;
import com.xnelo.filearch.restapi.service.UserService;
import com.xnelo.filearch.restapi.service.Utils;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class TagService {
  public static final String TAG_DATA_KEY = "TAG_DATA__TAG";

  @Inject UserService userService;
  @Inject TagRepo tagRepo;
  @Inject FileTagsRepo fileTagsRepo;
  @Inject GroupRepo groupRepo;
  @Inject SharedTagsRepo sharedTagsRepo;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);
  @Inject FileService fileService;

  public Uni<ServiceResponse<PaginatedResponse<Tag>>> getAllTags(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService.checkUserExist(
        requestContext,
        context2 ->
            tagRepo
                .getAll(context2.getUser().getId(), paginationParameters)
                .map(
                    paginatedTags ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(),
                                context2.getActionType(),
                                paginationMapper.toPaginatedResponse(paginatedTags)))));
  }

  Uni<ServiceResponse<Tag>> getTagByIdNoUserCheck(
      final ServiceRequestContext requestContext, final long tagId) {
    return tagRepo
        .getTagById(tagId, requestContext.getUser().getId())
        .map(
            tag -> {
              if (tag == null) {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        List.of(
                            ServiceError.builder()
                                .errorCode(ErrorCode.TAG_DOES_NOT_EXIST)
                                .errorMessage("Tag does not exist.")
                                .httpCode(404)
                                .build())));
              }

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(), requestContext.getActionType(), tag));
            });
  }

  public Uni<ServiceResponse<Tag>> getTagById(
      final ServiceRequestContext requestContext, final long tagId) {
    return userService.checkUserExist(
        requestContext, context2 -> getTagByIdNoUserCheck(context2, tagId));
  }

  public Uni<ServiceResponse<Tag>> createNewTag(
      final ServiceRequestContext requestContext, final TagContract newTag) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            tagRepo
                .tagNameExists(context2.getUser().getId(), newTag.getTagName())
                .chain(
                    nameExists -> {
                      if (nameExists) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        context2.getResourceType(),
                                        context2.getActionType(),
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
                          .createTag(context2.getUser().getId(), newTag.getTagName())
                          .map(
                              newlyCreatedTag ->
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          context2.getResourceType(),
                                          context2.getActionType(),
                                          newlyCreatedTag)));
                    }));
  }

  public Uni<ServiceResponse<Tag>> updateTag(
      final ServiceRequestContext requestContext, final long tagId, final TagContract tagData) {
    if (tagData.getId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
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
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.TAG_OWNER_CANNOT_BE_UPDATED)
                              .errorMessage("Tag owner cannot be updated.")
                              .httpCode(400)
                              .build()))));
    }

    return userService.checkUserExist(
        requestContext,
        context2 ->
            tagRepo
                .getTagById(tagId, context2.getUser().getId())
                .chain(
                    existingTag -> {
                      if (existingTag == null) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        context2.getResourceType(),
                                        context2.getActionType(),
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
                                        context2.getResourceType(),
                                        context2.getActionType(),
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.TAG_NO_UPDATES_EXECUTED)
                                                .errorMessage("No updates to execute on this tag.")
                                                .httpCode(400)
                                                .build()))));
                      }
                      // check if tag name exists for user
                      return tagRepo
                          .tagNameExists(context2.getUser().getId(), tagData.getTagName())
                          .chain(
                              nameExists -> {
                                if (nameExists) {
                                  return Uni.createFrom()
                                      .item(
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  context2.getResourceType(),
                                                  context2.getActionType(),
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
                                    .updateName(
                                        tagId, context2.getUser().getId(), tagData.getTagName())
                                    .map(
                                        updatedTag ->
                                            new ServiceResponse<>(
                                                new ServiceActionResponse<>(
                                                    context2.getResourceType(),
                                                    context2.getActionType(),
                                                    updatedTag)));
                              });
                    }));
  }

  public Uni<ServiceResponse<Tag>> deleteTag(
      final ServiceRequestContext requestContext, final long tagId) {
    return userService.checkUserExist(
        requestContext, context2 -> deleteIfTagExists(context2, tagId));
  }

  public Uni<ServiceResponse<Tag>> deleteIfTagExists(
      final ServiceRequestContext requestContext, final long tagId) {
    return getTagByIdNoUserCheck(requestContext, tagId)
        .chain(
            tagServiceResponse -> {
              if (tagServiceResponse.hasError()) {
                return updateErrorAndPassThrough(tagServiceResponse);
              }

              Tag tagData = tagServiceResponse.getActionResponses().getFirst().getData();

              return sharedTagsRepo
                  .deleteSharedTag(tagId)
                  .chain(
                      deleteSuccess -> {
                        if (!deleteSuccess) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          requestContext.getResourceType(),
                                          requestContext.getActionType(),
                                          List.of(
                                              ServiceError.builder()
                                                  .errorCode(ErrorCode.DB_ERROR)
                                                  .errorMessage(
                                                      "Error removing tag("
                                                          + tagId
                                                          + ") from shared tag table.")
                                                  .httpCode(500)
                                                  .build()))));
                        }

                        return fileTagsRepo
                            .deleteAllTagUses(tagId)
                            .chain(
                                deleteTagUsesSuccess -> {
                                  if (!deleteTagUsesSuccess) {
                                    return Uni.createFrom()
                                        .item(
                                            new ServiceResponse<>(
                                                new ServiceActionResponse<>(
                                                    requestContext.getResourceType(),
                                                    requestContext.getActionType(),
                                                    List.of(
                                                        ServiceError.builder()
                                                            .errorCode(
                                                                ErrorCode
                                                                    .TAG_USES_COULD_NOT_BE_DELETED)
                                                            .errorMessage(
                                                                "Error deleting tag uses from database '"
                                                                    + tagId
                                                                    + "'")
                                                            .httpCode(500)
                                                            .build()))));
                                  }

                                  return tagRepo
                                      .deleteTag(requestContext.getUser().getId(), tagId)
                                      .map(
                                          deleteTagSuccess -> {
                                            if (!deleteTagSuccess) {
                                              return new ServiceResponse<>(
                                                  new ServiceActionResponse<>(
                                                      requestContext.getResourceType(),
                                                      requestContext.getActionType(),
                                                      List.of(
                                                          ServiceError.builder()
                                                              .errorCode(
                                                                  ErrorCode
                                                                      .TAG_COULD_NOT_BE_DELETED)
                                                              .errorMessage(
                                                                  "Error deleting tag from database '"
                                                                      + tagId
                                                                      + "'")
                                                              .httpCode(500)
                                                              .build())));
                                            }
                                            return new ServiceResponse<>(
                                                new ServiceActionResponse<>(
                                                    requestContext.getResourceType(),
                                                    requestContext.getActionType(),
                                                    tagData));
                                          });
                                });
                      });
            });
  }

  public <T> Uni<ServiceResponse<T>> checkIfTagExists(
      final ServiceRequestContext requestContext,
      final long tagId,
      final Function<ServiceRequestContext, Uni<ServiceResponse<T>>> tagExistAction) {
    return tagRepo
        .getTagById(tagId, requestContext.getUser().getId())
        .chain(
            tag -> {
              if (tag == null) {
                return Uni.createFrom()
                    .item(
                        Utils.createServiceErrorResponse(
                            requestContext,
                            ErrorCode.TAG_DOES_NOT_EXIST,
                            "Tag (" + tagId + ") does not exist.",
                            404));
              }

              requestContext.setData(TAG_DATA_KEY, tag);
              return tagExistAction.apply(requestContext);
            });
  }

  public Uni<ServiceResponse<PaginatedResponse<Tag>>> getTagsAssignedToFile(
      final ServiceRequestContext requestContext,
      final Long fileId,
      final PaginationParameters paginationParameters) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            fileService.checkFileExists(
                context2,
                fileId,
                context3 ->
                    tagRepo
                        .getAllTagsForFile(context3.getUser().getId(), fileId, paginationParameters)
                        .map(
                            paginatedTags ->
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.TAG,
                                        ActionType.GET,
                                        paginationMapper.toPaginatedResponse(paginatedTags))))));
  }

  public Uni<ServiceResponse<List<Tag>>> searchTags(
      final ServiceRequestContext requestContext, final String searchText, final Integer limit) {
    if (limit != null && limit <= 0) {
      return Uni.createFrom()
          .item(
              Utils.createServiceErrorResponse(
                  requestContext,
                  ErrorCode.INVALID_RESPONSE_LIMIT,
                  "A return limit of '" + limit + "' is invalid. Must be greater than 0",
                  400));
    }

    return userService.checkUserExist(
        requestContext,
        context2 ->
            tagRepo
                .searchTags(context2.getUser().getId(), searchText, limit)
                .map(
                    tagList ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(), context2.getActionType(), tagList))));
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

  public Uni<ServiceResponse<TagShareResult>> shareTagsBulk(
      final ServiceRequestContext requestContext, final TagShareBulkContract tagsToShare) {
    return userService.checkUserExist(
        requestContext,
        context2 -> {
          ArrayList<Uni<ServiceActionResponse<TagShareResult>>> actions = new ArrayList<>();
          tagsToShare
              .getTagsToShare()
              .forEach(tagShare -> actions.add(shareTagIndividual(context2, tagShare)));
          return Uni.combine()
              .all()
              .unis(actions)
              .with(
                  toCombine ->
                      Utils.combineServiceActionResponses(toCombine, TagShareResult.class));
        });
  }

  Uni<ServiceActionResponse<TagShareResult>> shareTagIndividual(
      final ServiceRequestContext requestContext, final TagShareContract tagToShare) {
    return groupRepo
        .userActiveMemberInGroup(requestContext.getUser().getId(), tagToShare.getGroupId())
        .chain(
            isActiveMember -> {
              if (!isActiveMember) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            requestContext.getResourceType(),
                            requestContext.getActionType(),
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                    .errorMessage(
                                        "User "
                                            + requestContext.getUser().getId()
                                            + " is not active member of "
                                            + tagToShare.getGroupId()
                                            + " group")
                                    .httpCode(403)
                                    .build())));
              }

              return tagRepo
                  .getTagById(tagToShare.getTagId(), requestContext.getUser().getId())
                  .chain(
                      tagData -> {
                        if (tagData == null) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceActionResponse<>(
                                      requestContext.getResourceType(),
                                      requestContext.getActionType(),
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(ErrorCode.TAG_DOES_NOT_EXIST)
                                              .errorMessage(
                                                  "Tag("
                                                      + tagToShare.getTagId()
                                                      + ") doesn't exist")
                                              .httpCode(404)
                                              .build())));
                        }

                        return sharedTagsRepo
                            .tagShareExists(tagToShare.getTagId(), tagToShare.getGroupId())
                            .chain(
                                tagShareExists -> {
                                  if (tagShareExists) {
                                    return Uni.createFrom()
                                        .item(
                                            new ServiceActionResponse<>(
                                                requestContext.getResourceType(),
                                                requestContext.getActionType(),
                                                List.of(
                                                    ServiceError.builder()
                                                        .errorCode(ErrorCode.TAG_SHARE_EXISTS)
                                                        .errorMessage(
                                                            "Tag "
                                                                + tagToShare.getTagId()
                                                                + " already shared with group "
                                                                + tagToShare.getGroupId())
                                                        .httpCode(400)
                                                        .build())));
                                  }

                                  return sharedTagsRepo
                                      .addSharedTag(tagToShare.getTagId(), tagToShare.getGroupId())
                                      .map(
                                          insertSuccess ->
                                              new ServiceActionResponse<>(
                                                  requestContext.getResourceType(),
                                                  requestContext.getActionType(),
                                                  new TagShareResult(
                                                      tagToShare.getTagId(),
                                                      tagToShare.getGroupId(),
                                                      insertSuccess)));
                                });
                      });
            });
  }

  public Uni<ServiceResponse<TagShareResult>> unshareTagsBulk(
      final ServiceRequestContext requestContext, final TagShareBulkContract tagsToUnshare) {
    return userService.checkUserExist(
        requestContext,
        context2 -> {
          ArrayList<Uni<ServiceActionResponse<TagShareResult>>> actions = new ArrayList<>();
          tagsToUnshare
              .getTagsToShare()
              .forEach(tagShare -> actions.add(unshareTagIndividual(context2, tagShare)));
          return Uni.combine()
              .all()
              .unis(actions)
              .with(
                  toCombine ->
                      Utils.combineServiceActionResponses(toCombine, TagShareResult.class));
        });
  }

  Uni<ServiceActionResponse<TagShareResult>> unshareTagIndividual(
      final ServiceRequestContext requestContext, final TagShareContract tagToUnshare) {
    return tagRepo
        .getTagById(tagToUnshare.getTagId(), requestContext.getUser().getId())
        .chain(
            tagData -> {
              if (tagData == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            requestContext.getResourceType(),
                            requestContext.getActionType(),
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.TAG_DOES_NOT_EXIST)
                                    .errorMessage(
                                        "Tag (" + tagToUnshare.getTagId() + ") doesn't exist")
                                    .httpCode(404)
                                    .build())));
              }

              return groupRepo
                  .userActiveMemberInGroup(
                      requestContext.getUser().getId(), tagToUnshare.getGroupId())
                  .chain(
                      isActiveMember -> {
                        if (!isActiveMember) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceActionResponse<>(
                                      requestContext.getResourceType(),
                                      requestContext.getActionType(),
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(ErrorCode.USER_NOT_ACTIVE)
                                              .errorMessage(
                                                  "User "
                                                      + requestContext.getUser().getId()
                                                      + " is not active member of group "
                                                      + tagToUnshare.getGroupId())
                                              .httpCode(400)
                                              .build())));
                        }

                        return sharedTagsRepo
                            .unshareTag(tagToUnshare.getTagId(), tagToUnshare.getGroupId())
                            .map(
                                dbSuccess ->
                                    new ServiceActionResponse<>(
                                        requestContext.getResourceType(),
                                        requestContext.getActionType(),
                                        new TagShareResult(
                                            tagToUnshare.getTagId(),
                                            tagToUnshare.getGroupId(),
                                            dbSuccess)));
                      });
            });
  }
}
