package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.user.UserHandler;
import com.xnelo.filearch.restapi.api.contracts.FileResource;
import io.quarkus.logging.Log;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("upload")
public class UploadResource {

  @Inject UserHandler userhandler;

  @POST
  @RolesAllowed("user")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(@BeanParam FileResource upload) {
    Log.infof("File path: %s", upload.file.getAbsolutePath());
    Log.infof("Calling User: %s", userhandler.getUserInfo().getId());
    return Response.ok().build();
  }
}
