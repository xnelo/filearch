package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.DownloadData;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.SearchParameters;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.AssignTagContract;
import com.xnelo.filearch.restapi.api.contracts.FileBulkDeleteContract;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.api.contracts.PaginationRequest;
import com.xnelo.filearch.restapi.api.contracts.SearchRequest;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.api.mappers.HttpStatusCodeMapper;
import com.xnelo.filearch.restapi.service.FileService;
import com.xnelo.filearch.restapi.service.tag.TagService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("file")
public class FileResource {
  @Inject UserTokenHandler userTokenHandler;
  @Inject FileService fileService;
  @Inject TagService tagService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @RolesAllowed("user")
  public Uni<Response> getAll(@BeanParam PaginationRequest pr) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    PaginationParameters paginationParameters = contractMapper.toPaginationParameters(pr);

    return fileService
        .getAllFiles(requestContext, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toFileContractList)));
  }

  @POST
  @RolesAllowed("user")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Response> uploadFile(FileUploadContract upload) {
    UserToken userInfo = userTokenHandler.getUserInfo();

    Log.debugf("Upload Location: folder_id=%d", upload.folderId);
    Log.debug("FILES >>>>>>>>>>>>>>>>");
    for (FileUpload file : upload.files) {
      Log.debugf("Filename: %s", file.fileName());
      Log.debugf("Temporary file path: %s", file.filePath());
      Log.debugf("Size: %d", file.size());
      Log.debug("---------");
    }
    Log.debug("<<<<<<<<<<<<<<<<<<<<<<");
    Log.debugf("Uploaded by: %s", userInfo.getId());

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.UPLOAD)
            .userToken(userInfo)
            .build();

    return fileService
        .uploadFiles(requestContext, upload)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFileContract));
  }

  @GET
  @Path("{id}")
  @RolesAllowed("user")
  public Uni<Response> getFileMetadata(@PathParam("id") long fileId) {
    UserToken userInfo = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .userToken(userInfo)
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.GET)
            .build();

    return fileService
        .getFileMetadata(requestContext, fileId)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFileContract));
  }

  @DELETE
  @Path("{id}")
  @RolesAllowed("user")
  public Uni<Response> deleteFile(@PathParam("id") long fileId) {
    UserToken userInfo = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .userToken(userInfo)
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.DELETE)
            .build();

    return fileService
        .deleteFile(requestContext, fileId)
        .map(
            fileServiceResponse ->
                contractMapper.toApiResponse(fileServiceResponse, contractMapper::toFileContract));
  }

  @GET
  @Path("{id}/download")
  @RolesAllowed("user")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Uni<Response> downloadFile(
      @PathParam("id") long fileId, @QueryParam("group_id") Long groupId) {
    UserToken userInfo = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.DOWNLOAD)
            .userToken(userInfo)
            .groupId(groupId)
            .addData(FileService.FILE_ID_KEY, fileId)
            .addData(FileService.GET_THUMBNAIL_KEY, Boolean.FALSE)
            .build();

    return fileService.getFileForDownload(requestContext).map(FileResource::mapToDownloadResponse);
  }

  @GET
  @Path("{id}/download_thumbnail")
  @RolesAllowed("user")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Uni<Response> downloadThumbnail(
      @PathParam("id") long fileId, @QueryParam("group_id") Long groupId) {
    UserToken userInfo = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.DOWNLOAD)
            .userToken(userInfo)
            .groupId(groupId)
            .addData(FileService.GET_THUMBNAIL_KEY, Boolean.TRUE)
            .addData(FileService.FILE_ID_KEY, fileId)
            .build();

    return fileService.getFileForDownload(requestContext).map(FileResource::mapToDownloadResponse);
  }

  private static Response mapToDownloadResponse(
      ServiceResponse<DownloadData> downloadFileServiceResponse) {
    if (downloadFileServiceResponse.getActionResponses().size() != 1) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    ServiceActionResponse<DownloadData> a =
        downloadFileServiceResponse.getActionResponses().getFirst();
    if (a.hasErrors()) {
      int finalHttpStatus = HttpStatusCodeMapper.INITIAL_STATUS_CODE;
      for (ServiceError error : a.getErrors()) {
        Log.errorf(
            "ErrorCode=%d ErrorMessage=%s",
            error.getErrorCode().getCode(), error.getErrorMessage());
        finalHttpStatus =
            HttpStatusCodeMapper.combineStatusCode(finalHttpStatus, error.getHttpCode());
      }
      return Response.status(finalHttpStatus).build();
    } else {
      DownloadData downloadData = a.getData();
      return Response.ok(downloadData.getData())
          .header(
              "Content-Disposition", "attachment; filename=\"" + downloadData.getFilename() + "\"")
          .build();
    }
  }

  @POST
  @RolesAllowed("user")
  @Path("bulk/delete")
  public Uni<Response> bulkDeleteFiles(final FileBulkDeleteContract toDelete) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .userToken(userToken)
            .actionType(ActionType.DELETE)
            .resourceType(ResourceType.FILE)
            .build();

    return fileService
        .bulkDeleteFiles(requestContext, toDelete.fileIdsToDelete(), true)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFileContract));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/assign_tag")
  public Uni<Response> assignTag(@PathParam("id") long fileId, final AssignTagContract assignTag) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.ASSIGN)
            .userToken(userToken)
            .build();

    return fileService
        .assignTag(requestContext, fileId, assignTag.tagId())
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, (Boolean isSuccessful) -> isSuccessful));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/unassign_tag")
  public Uni<Response> unassignTag(
      @PathParam("id") final long fileId, final AssignTagContract unassignTag) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.UNASSIGN)
            .userToken(userToken)
            .build();

    return fileService
        .unassignTag(requestContext, fileId, unassignTag.tagId())
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, (Boolean isSuccessful) -> isSuccessful));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/tags")
  public Uni<Response> getFileTags(
      @PathParam("id") long fileId, @BeanParam PaginationRequest paginationRequest) {
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
        .getTagsAssignedToFile(requestContext, fileId, paginationParameters)
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
  @Path("search")
  public Uni<Response> searchFiles(@BeanParam SearchRequest searchRequest) {
    UserToken userToken = userTokenHandler.getUserInfo();
    SearchParameters searchParameters = contractMapper.toSearchParameters(searchRequest);

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.FILE)
            .actionType(ActionType.SEARCH)
            .userToken(userToken)
            .build();

    return fileService
        .searchFiles(requestContext, searchParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toFileContractList)));
  }
}
