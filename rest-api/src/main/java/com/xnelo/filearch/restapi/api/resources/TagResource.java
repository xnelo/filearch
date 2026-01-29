package com.xnelo.filearch.restapi.api.resources;

import static com.xnelo.filearch.common.json.JsonUtil.toJsonString;

import com.xnelo.filearch.common.model.SortDirection;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.TagContract;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.TagService;
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
  public Uni<Response> getAll(
      @QueryParam("after") Long after,
      @QueryParam("limit") Integer limit,
      @QueryParam("direction") SortDirection dir) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return tagService
        .getAllTags(userToken, after, limit, dir)
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
    return tagService
        .searchTags(userToken, searchText, limit)
        .map(
            serviceResponseList ->
                contractMapper.toApiResponse(
                    serviceResponseList, contractMapper::toTagContractList));
  }

  @POST
  @RolesAllowed("user")
  public Uni<Response> createNewTag(final TagContract newTag) {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.debugf(
        "Creating new Tag: input=%s token=%s", toJsonString(newTag), toJsonString(userToken));
    return tagService
        .createNewTag(newTag, userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toTagContract));
  }

  @PATCH
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> updateTag(@PathParam("id") long tagId, final TagContract tagData) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return tagService
        .updateTag(tagId, userToken, tagData)
        .map(
            tagServiceResponse ->
                contractMapper.toApiResponse(tagServiceResponse, contractMapper::toTagContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> getTagById(@PathParam("id") long tagId) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return tagService
        .getTagById(tagId, userToken)
        .map(
            tagServiceResponse ->
                contractMapper.toApiResponse(tagServiceResponse, contractMapper::toTagContract));
  }

  @DELETE
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> deleteTag(@PathParam("id") long tagId) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return tagService
        .deleteTag(userToken, tagId)
        .map(
            tagServiceResponse ->
                contractMapper.toApiResponse(tagServiceResponse, contractMapper::toTagContract));
  }
}
