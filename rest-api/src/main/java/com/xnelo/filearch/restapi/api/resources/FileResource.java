package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.model.DownloadData;
import com.xnelo.filearch.common.model.SortDirection;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.FileBulkDeleteContract;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.api.mappers.HttpStatusCodeMapper;
import com.xnelo.filearch.restapi.service.FileService;
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
  @Inject UserTokenHandler userhandler;
  @Inject FileService fileService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @RolesAllowed("user")
  public Uni<Response> getAll(
      @QueryParam("after") Long after,
      @QueryParam("limit") Integer limit,
      @QueryParam("direction") SortDirection dir) {
    UserToken userToken = userhandler.getUserInfo();
    return fileService
        .getAllFiles(userToken, after, limit, dir)
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
    Log.debugf("Upload Location: folder_id=%d", upload.folderId);
    Log.debug("FILES >>>>>>>>>>>>>>>>");
    for (FileUpload file : upload.files) {
      Log.debugf("Filename: %s", file.fileName());
      Log.debugf("Temporary file path: %s", file.filePath());
      Log.debugf("Size: %d", file.size());
      Log.debug("---------");
    }
    Log.debug("<<<<<<<<<<<<<<<<<<<<<<");
    Log.debugf("Uploaded by: %s", userhandler.getUserInfo().getId());
    return fileService
        .uploadFiles(upload, userhandler.getUserInfo())
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFileContract));
  }

  @GET
  @Path("{id}")
  @RolesAllowed("user")
  public Uni<Response> getFileMetadata(@PathParam("id") long fileId) {
    UserToken userInfo = userhandler.getUserInfo();
    return fileService
        .getFileMetadata(fileId, userInfo)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFileContract));
  }

  @DELETE
  @Path("{id}")
  @RolesAllowed("user")
  public Uni<Response> deleteFile(@PathParam("id") long fileId) {
    UserToken userInfo = userhandler.getUserInfo();
    return fileService
        .deleteFile(fileId, userInfo)
        .map(
            fileServiceResponse ->
                contractMapper.toApiResponse(fileServiceResponse, contractMapper::toFileContract));
  }

  @GET
  @Path("{id}/download")
  @RolesAllowed("user")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Uni<Response> downloadFile(@PathParam("id") long fileId) {
    UserToken userInfo = userhandler.getUserInfo();
    return fileService
        .getFileForDownload(fileId, userInfo)
        .map(FileResource::mapToDownloadResponse);
  }

  @GET
  @Path("{id}/download_thumbnail")
  @RolesAllowed("user")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Uni<Response> downloadThumbnail(@PathParam("id") long fileId) {
    UserToken userInfo = userhandler.getUserInfo();
    return fileService
        .getFileThumbnailForDownload(fileId, userInfo)
        .map(FileResource::mapToDownloadResponse);
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
        Log.errorf("ErrorCode=%d ErrorMessage=%s", error.getErrorCode().getCode(), error.getErrorMessage());
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
    UserToken userToken = userhandler.getUserInfo();
    return fileService
        .bulkDeleteFiles(toDelete.fileIdsToDelete(), userToken)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toFileContract));
  }
}
