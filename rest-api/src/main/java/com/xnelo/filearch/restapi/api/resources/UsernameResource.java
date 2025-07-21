package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.UserService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("username")
public class UsernameResource {
  @Inject UserService userService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @Path("available")
  public Uni<Response> isUsernameAvailable(@QueryParam("username") String username) {
    return userService
        .isUsernameAvailable(username)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, (Boolean isAvailable) -> isAvailable));
  }
}
