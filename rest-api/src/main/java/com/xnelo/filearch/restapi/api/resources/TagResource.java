package com.xnelo.filearch.restapi.api.resources;

import static com.xnelo.filearch.common.json.JsonUtil.toJsonString;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.PaginationRequest;
import com.xnelo.filearch.restapi.api.contracts.TagContract;
import com.xnelo.filearch.restapi.api.contracts.TagShareBulkContract;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.tag.TagService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("tag")
public class TagResource {
  @Inject UserTokenHandler userTokenHandler;
  @Inject TagService tagService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @RolesAllowed("user")
  public Uni<Response> getAll(@BeanParam PaginationRequest paginationRequest) {
    UserToken userToken = userTokenHandler.getUserInfo();
    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    return tagService
        .getAllTags(requestContext, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toTagContractList)));
  }

  @GET
  @RolesAllowed("user")
  @Path("/search")
  public Uni<Response> search(
      @QueryParam("search_text") String searchText, @QueryParam("limit") Integer limit) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.SEARCH)
            .userToken(userToken)
            .build();

    return tagService
        .searchTags(requestContext, searchText, limit)
        .map(
            serviceResponseList ->
                contractMapper.toApiResponse(
                    serviceResponseList, contractMapper::toTagContractList));
  }

  @POST
  @RolesAllowed("user")
  @Path("/share")
  public Uni<Response> share(final TagShareBulkContract tagsToShare) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.SHARE_TAG)
            .userToken(userToken)
            .build();

    return tagService
        .shareTagsBulk(requestContext, tagsToShare)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toTagShareResponse));
  }

  @POST
  @RolesAllowed("user")
  @Path("/unshare")
  public Uni<Response> unshare(final TagShareBulkContract tagsToUnshare) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.SHARE_TAG)
            .userToken(userToken)
            .build();

    return tagService
        .unshareTagsBulk(requestContext, tagsToUnshare)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toTagShareResponse));
  }

  @POST
  @RolesAllowed("user")
  public Uni<Response> createNewTag(final TagContract newTag) {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.debugf(
        "Creating new Tag: input=%s token=%s", toJsonString(newTag), toJsonString(userToken));

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.CREATE)
            .userToken(userToken)
            .build();

    return tagService
        .createNewTag(requestContext, newTag)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toTagContract));
  }

  @PATCH
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> updateTag(@PathParam("id") long tagId, final TagContract tagData) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.UPDATE)
            .userToken(userToken)
            .build();

    return tagService
        .updateTag(requestContext, tagId, tagData)
        .map(
            tagServiceResponse ->
                contractMapper.toApiResponse(tagServiceResponse, contractMapper::toTagContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> getTagById(@PathParam("id") long tagId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    return tagService
        .getTagById(requestContext, tagId)
        .map(
            tagServiceResponse ->
                contractMapper.toApiResponse(tagServiceResponse, contractMapper::toTagContract));
  }

  @DELETE
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> deleteTag(@PathParam("id") long tagId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.DELETE)
            .userToken(userToken)
            .build();

    return tagService
        .deleteTag(requestContext, tagId)
        .map(
            tagServiceResponse ->
                contractMapper.toApiResponse(tagServiceResponse, contractMapper::toTagContract));
  }
}
