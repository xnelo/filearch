package com.xnelo.filearch.restapi.api.resources;

import static com.xnelo.filearch.common.json.JsonUtil.toJsonString;

import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.UserContract;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.UserService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("user")
public class UserResource {
  @Inject UserTokenHandler userTokenHandler;
  @Inject UserService userService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> getUser(@PathParam("id") int userId) {
    return userService
        .getUserById(userId)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toUserContract));
  }

  @POST
  @RolesAllowed("user")
  public Uni<Response> createUser(@Valid final UserContract userContract) {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.infof(
        "Create User: input=%s token=%s", toJsonString(userContract), toJsonString(userToken));
    return userService
        .createUser(userContract, userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toUserContract));
  }

  @PATCH
  @RolesAllowed("user")
  public Uni<Response> updateUser(@Valid final UserContract userContract) {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.infof(
        "Updating user: input=%s token=%s", toJsonString(userContract), toJsonString(userToken));
    return userService
        .updateUser(userContract, userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toUserContract));
  }

  @DELETE
  @RolesAllowed("user")
  public Uni<Response> deleteUser() {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.infof("Deleting user: token=%s", toJsonString(userToken));
    return userService
        .deleteUser(userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toUserContract));
  }

  @GET
  @RolesAllowed("user")
  public Uni<Response> getUserByExternalId() {
    UserToken userToken = userTokenHandler.getUserInfo();
    return userService
        .getUserFromUserToken(userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toUserContract));
  }
}
