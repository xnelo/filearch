package com.xnelo.filearch.restapi.api.contracts;

import jakarta.ws.rs.FormParam;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FileUploadContract {
  @FormParam("files")
  public List<FileUpload> files;

  @FormParam("location")
  public String location;
}
