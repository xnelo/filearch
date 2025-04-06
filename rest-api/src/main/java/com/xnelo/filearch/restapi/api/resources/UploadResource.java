package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.UploadFileResource;
import com.xnelo.filearch.restapi.service.UploadService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("upload")
public class UploadResource {

  @Inject
  UserTokenHandler userhandler;
  @Inject UploadService uploadService;

  @POST
  @RolesAllowed("user")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Response> uploadFile(@BeanParam UploadFileResource upload) {
    Log.debugf("Temporary file path: %s", upload.file.filePath());
    Log.infof("Calling User: %s", userhandler.getUserInfo().getId());
    return uploadService
        .uploadFile(upload, userhandler.getUserInfo())
        .map(result -> Response.ok(result).build());
  }
}
