package com.xnelo.filearch.restapi.api.contracts;

import jakarta.ws.rs.FormParam;
import java.io.File;

public class FileResource {
  @FormParam("file")
  public File file;
}
