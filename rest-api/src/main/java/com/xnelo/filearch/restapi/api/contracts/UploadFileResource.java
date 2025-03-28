package com.xnelo.filearch.restapi.api.contracts;

import jakarta.ws.rs.FormParam;
import java.io.File;

public class UploadFileResource {
  @FormParam("file")
  public File file;
}
