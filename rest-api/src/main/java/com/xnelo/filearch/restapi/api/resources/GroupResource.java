package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.GroupCreateContract;
import com.xnelo.filearch.restapi.api.contracts.PaginationRequest;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.GroupService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("group")
public class GroupResource {
  @Inject UserTokenHandler userTokenHandler;
  @Inject GroupService groupService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @RolesAllowed("user")
  public Uni<Response> getAll(@BeanParam PaginationRequest paginationRequest) {
    UserToken userToken = userTokenHandler.getUserInfo();
    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);
    return groupService
        .getAllGroups(userToken, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toGroupContractList)));
  }

  @POST
  @RolesAllowed("user")
  public Uni<Response> createNewGroup(final GroupCreateContract newGroup) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return groupService
        .createNewGroup(newGroup, userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toGroupContract));
  }

  @DELETE
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> deleteGroup(@PathParam("id") long groupId) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return groupService
        .deleteGroup(userToken, groupId)
        .map(
            groupServiceResponse ->
                contractMapper.toApiResponse(
                    groupServiceResponse, contractMapper::toGroupContract));
  }
}
