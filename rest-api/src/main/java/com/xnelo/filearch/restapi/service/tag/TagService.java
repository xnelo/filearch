package com.xnelo.filearch.restapi.service.tag;

import com.xnelo.filearch.common.exception.RepoException;
import com.xnelo.filearch.common.exception.ServiceResponseException;
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
import com.xnelo.filearch.restapi.data.PaginatedData;
import com.xnelo.filearch.restapi.data.SharedTagsRepo;
import com.xnelo.filearch.restapi.data.TagRepo;
import com.xnelo.filearch.restapi.service.FileService;
import com.xnelo.filearch.restapi.service.GroupItemService;
import com.xnelo.filearch.restapi.service.GroupService;
import com.xnelo.filearch.restapi.service.SharedTagsService;
import com.xnelo.filearch.restapi.service.UserService;
import com.xnelo.filearch.restapi.service.Utils;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@RequestScoped
public class TagService {
  public static final String TAG_DATA_KEY = "TAG_DATA__TAG";

  private final FileService fileService;
  private final GroupService groupService;
  private final GroupItemService groupItemService;
  private final SharedTagsService sharedTagsService;
  private final UserService userService;
  private final FileTagsRepo fileTagsRepo;
  private final SharedTagsRepo sharedTagsRepo;
  private final TagRepo tagRepo;
  private final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  @Inject
  public TagService(
      final FileService fileService,
      final GroupService groupService,
      final GroupItemService groupItemService,
      final SharedTagsService sharedTagsService,
      final UserService userService,
      final FileTagsRepo fileTagsRepo,
      final SharedTagsRepo sharedTagsRepo,
      final TagRepo tagRepo) {
    this.fileService = fileService;
    this.groupService = groupService;
    this.groupItemService = groupItemService;
    this.sharedTagsService = sharedTagsService;
    this.userService = userService;
    this.fileTagsRepo = fileTagsRepo;
    this.sharedTagsRepo = sharedTagsRepo;
    this.tagRepo = tagRepo;
  }

  public Uni<ServiceResponse<PaginatedResponse<Tag>>> getAllTags(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService
        .checkUserExist(requestContext)
        .chain(
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

  public Uni<ServiceResponse<Tag>> getTagById(
      final ServiceRequestContext requestContext, final long tagId) {
    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> tagRepo.getTagById(tagId, requestContext.getUser().getId()))
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

  public Uni<ServiceResponse<Tag>> createNewTag(
      final ServiceRequestContext requestContext, final TagContract newTag) {
    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> tagRepo.tagNameExists(context2.getUser().getId(), newTag.getTagName()))
        .chain(
            nameExists -> {
              if (nameExists) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.TAG_WITH_NAME_ALREADY_EXISTS,
                    "A Tag with the name '" + newTag.getTagName() + "' already exists",
                    400);
              }

              // All checks passed... insert new tag
              return tagRepo.createTag(requestContext.getUser().getId(), newTag.getTagName());
            })
        .map(
            newlyCreatedTag ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        newlyCreatedTag)));
  }

  public Uni<ServiceResponse<Tag>> updateTag(
      final ServiceRequestContext requestContext, final long tagId, final TagContract tagData) {
    if (tagData.getId() != null) {
      throw new ServiceResponseException(
          requestContext, ErrorCode.TAG_ID_CANNOT_BE_UPDATED, "Tag id cannot be updated.", 400);
    } else if (tagData.getOwnerId() != null) {
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.TAG_OWNER_CANNOT_BE_UPDATED,
          "Tag owner cannot be updated.",
          400);
    }

    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> checkIfTagExists(context2, tagId))
        .invoke(
            context -> {
              if (tagData.getTagName() == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.TAG_NO_UPDATES_EXECUTED,
                    "No updates to execute on this tag.",
                    400);
              }
            })
        .chain(context -> tagRepo.tagNameExists(context.getUser().getId(), tagData.getTagName()))
        .invoke(
            nameExists -> {
              if (nameExists) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.TAG_WITH_NAME_ALREADY_EXISTS,
                    "A tag with name '" + tagData.getTagName() + "' already exists.",
                    400);
              }
            })
        .chain(
            _ignore ->
                tagRepo.updateName(tagId, requestContext.getUser().getId(), tagData.getTagName()))
        .map(
            updatedTag ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        updatedTag)));
  }

  public Uni<ServiceResponse<Tag>> deleteTag(
      final ServiceRequestContext requestContext, final long tagId) {
    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> deleteIfTagExists(context2, tagId));
  }

  public Uni<ServiceResponse<Tag>> deleteIfTagExists(
      final ServiceRequestContext requestContext, final long tagId) {
    return checkIfTagExists(requestContext, tagId)
        .chain(context -> sharedTagsRepo.deleteSharedTag(tagId))
        .chain(_ignored -> fileTagsRepo.deleteAllTagUses(tagId))
        .chain(_ignored -> tagRepo.deleteTag(requestContext.getUser().getId(), tagId))
        .map(
            _ignored -> {
              Tag tagData = requestContext.getDataAs(TAG_DATA_KEY, Tag.class);

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(), requestContext.getActionType(), tagData));
            })
        .onFailure(RepoException.class)
        .transform(
            ex ->
                new ServiceResponseException(
                    requestContext, ex.getErrorCode(), ex.getMessage(), 500));
  }

  public Uni<ServiceRequestContext> checkIfTagExists(
      ServiceRequestContext requestContext, final long tagId) {
    Utils.checkUserInRequest(requestContext);

    return tagRepo
        .getTagById(tagId, requestContext.getUser().getId())
        .map(
            tag -> {
              if (tag == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.TAG_DOES_NOT_EXIST,
                    "Tag (" + tagId + ") does not exist.",
                    404);
              }

              requestContext.setData(TAG_DATA_KEY, tag);

              return requestContext;
            });
  }

  public Uni<ServiceResponse<PaginatedResponse<Tag>>> getTagsAssignedToFile(
      final ServiceRequestContext requestContext,
      final Long fileId,
      final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    Uni<PaginatedData<Tag>> getTags;

    if (requestContext.getGroupId() != null) {
      getTags =
          userService
              .checkUserExist(requestContext)
              .chain(groupService::checkUserActiveMember)
              .chain(
                  context -> groupItemService.checkItemInGroup(context, fileId, GroupItemType.FILE))
              .chain(
                  context ->
                      tagRepo.getAllTagsInGroupForFile(
                          fileId, requestContext.getGroupId(), paginationParameters));
    } else {
      getTags =
          userService
              .checkUserExist(requestContext)
              .chain(context2 -> fileService.checkFileExist(context2, fileId))
              .chain(
                  context ->
                      tagRepo.getAllTagsForFile(
                          context.getUser().getId(), fileId, paginationParameters));
    }

    return getTags.map(
        paginatedTags ->
            new ServiceResponse<>(
                new ServiceActionResponse<>(
                    requestContext.getResourceType(),
                    requestContext.getActionType(),
                    paginationMapper.toPaginatedResponse(paginatedTags))));
  }

  public Uni<ServiceResponse<List<Tag>>> searchTags(
      final ServiceRequestContext requestContext, final String searchText, final Integer limit) {
    if (limit != null && limit <= 0) {
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.INVALID_RESPONSE_LIMIT,
          "A return limit of '" + limit + "' is invalid. Must be greater than 0",
          400);
    }

    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> tagRepo.searchTags(context2.getUser().getId(), searchText, limit))
        .map(
            tagList ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        tagList)));
  }

  public Uni<ServiceResponse<TagShareResult>> shareTagsBulk(
      final ServiceRequestContext requestContext, final TagShareBulkContract tagsToShare) {
    return userService
        .checkUserExist(requestContext)
        .chain(
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
    return groupService
        .checkUserActiveMember(requestContext, tagToShare.getGroupId())
        .chain(context -> checkIfTagExists(context, tagToShare.getTagId()))
        .chain(
            context ->
                sharedTagsService.checkTagShareNotExists(
                    context, tagToShare.getTagId(), tagToShare.getGroupId()))
        .chain(
            context -> sharedTagsRepo.addSharedTag(tagToShare.getTagId(), tagToShare.getGroupId()))
        .map(
            insertSuccess ->
                new ServiceActionResponse<>(
                    requestContext.getResourceType(),
                    requestContext.getActionType(),
                    new TagShareResult(
                        tagToShare.getTagId(), tagToShare.getGroupId(), insertSuccess)))
        .onFailure(ServiceResponseException.class)
        .invoke(ex -> log.error(ex.getMessage(), ex))
        .onFailure(ServiceResponseException.class)
        .recoverWithItem(ServiceResponseException::toServiceActionResponse);
  }

  public Uni<ServiceResponse<TagShareResult>> unshareTagsBulk(
      final ServiceRequestContext requestContext, final TagShareBulkContract tagsToUnshare) {
    return userService
        .checkUserExist(requestContext)
        .chain(
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
    return checkIfTagExists(requestContext, tagToUnshare.getTagId())
        .chain(context -> groupService.checkUserActiveMember(context, tagToUnshare.getGroupId()))
        .chain(
            context ->
                sharedTagsRepo.unshareTag(tagToUnshare.getTagId(), tagToUnshare.getGroupId()))
        .map(
            dbSuccess ->
                new ServiceActionResponse<>(
                    requestContext.getResourceType(),
                    requestContext.getActionType(),
                    new TagShareResult(
                        tagToUnshare.getTagId(), tagToUnshare.getGroupId(), dbSuccess)))
        .onFailure(ServiceResponseException.class)
        .recoverWithItem(ServiceResponseException::toServiceActionResponse);
  }

  public Uni<ServiceResponse<List<Long>>> getGroupsTagIsIn(
      ServiceRequestContext requestContext, final long tagId) {

    return userService
        .checkUserExist(requestContext)
        .chain(context -> sharedTagsRepo.groupsTagIsIn(requestContext.getUser().getId(), tagId))
        .map(
            response ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        response)))
        .onFailure(RepoException.class)
        .transform(
            ex ->
                new ServiceResponseException(
                    requestContext, ErrorCode.DB_ERROR, ex.getMessage(), 500));
  }
}
