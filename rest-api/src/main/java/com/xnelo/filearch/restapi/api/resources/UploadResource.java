package com.xnelo.filearch.restapi.api.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("upload")
public class UploadResource {
  @POST
  @Path("{test-thingy}")
  public Response uploadFile(@PathParam("test-thingy") Long testthingy) {
    return Response.ok().build();
  }
}
