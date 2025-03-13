package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.restapi.api.contracts.FileResource;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("upload")
public class UploadResource {

  @Inject SecurityIdentity securityIdentity;

  @POST
  @RolesAllowed("user")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(@BeanParam FileResource upload) {
    Log.infof("File path: %s", upload.file.getAbsolutePath());
    Log.infof("Calling User: %s", securityIdentity.getPrincipal());
    return Response.ok().build();
  }
}
