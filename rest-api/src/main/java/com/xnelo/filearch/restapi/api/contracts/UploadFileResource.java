package com.xnelo.filearch.restapi.api.contracts;

import jakarta.ws.rs.FormParam;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class UploadFileResource {
  @FormParam("file")
  public FileUpload file;
}
