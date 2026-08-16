package com.xnelo.filearch.restapi.api.resources;

import static com.xnelo.filearch.common.json.JsonUtil.toJsonString;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.FolderContract;
import com.xnelo.filearch.restapi.api.contracts.PaginationRequest;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.folder.FolderService;
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

  @GET
  @RolesAllowed("user")
  public Uni<Response> getAll(@BeanParam PaginationRequest paginationRequest) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FOLDER)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);
    return folderService
        .getAllFolders(requestContext, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toFolderContractList)));
  }

  @POST
  @RolesAllowed("user")
  public Uni<Response> createNewFolder(final FolderContract newFolder) {
    UserToken userToken = userTokenHandler.getUserInfo();
    Log.debugf(
        "Creating new Folder: input=%s token=%s", toJsonString(newFolder), toJsonString(userToken));

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FOLDER)
            .actionType(ActionType.CREATE)
            .userToken(userToken)
            .build();

    return folderService
        .createNewFolder(requestContext, newFolder)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFolderContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> getFolder(@PathParam("id") long folderId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FOLDER)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    return folderService
        .getFolderById(requestContext, folderId)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFolderContract));
  }

  @PATCH
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> updateFolder(
      @PathParam("id") long folderId, final FolderContract folderData) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FOLDER)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    return folderService
        .updateFolder(requestContext, folderId, folderData)
        .map(
            folderServiceResponse ->
                contractMapper.toApiResponse(
                    folderServiceResponse, contractMapper::toFolderContract));
  }

  @DELETE
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> deleteFolder(@PathParam("id") long folderId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FOLDER)
            .actionType(ActionType.DELETE)
            .userToken(userToken)
            .build();

    return folderService
        .deleteFolder(requestContext, folderId)
        .map(
            folderServiceDeleted ->
                contractMapper.toApiResponse(
                    folderServiceDeleted, contractMapper::toFolderContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/files")
  public Uni<Response> getFilesInFolder(
      @PathParam("id") long folderId, @BeanParam PaginationRequest paginationRequest) {
    UserToken userToken = userTokenHandler.getUserInfo();
    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);
    return folderService
        .getAllFilesInFolder(userToken, folderId, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toFileContractList)));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/files/all_ids")
  public Uni<Response> getAllFileIdsInFolder(@PathParam("id") long folderId) {
    UserToken userToken = userTokenHandler.getUserInfo();
    return folderService
        .getAllFileIdsInFolder(userToken, folderId)
        .map(idResponse -> contractMapper.toApiResponse(idResponse, ids -> ids));
  }
}
