package com.xnelo.filearch.restapi.api.resources;

import static com.xnelo.filearch.common.json.JsonUtil.toJsonString;

import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.FolderContract;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.FolderService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("folder")
public class FolderResource {
  @Inject UserTokenHandler userTokenHandler;
  @Inject FolderService folderService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @POST
  @RolesAllowed("user")
  public Uni<Response> createNewFolder(final FolderContract newFolder) {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.debugf(
        "Creating new Folder: input=%s token=%s", toJsonString(newFolder), toJsonString(userToken));
    return folderService
        .createNewFolder(newFolder, userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFolderContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> getFolder(@PathParam("id") long folderId) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return folderService
        .getFolderById(folderId, userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFolderContract));
  }
}
