package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.FileService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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

  @POST
  @RolesAllowed("user")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Response> uploadFile(FileUploadContract upload) {
    Log.debugf("Upload Location: %s", upload.location);
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
}
