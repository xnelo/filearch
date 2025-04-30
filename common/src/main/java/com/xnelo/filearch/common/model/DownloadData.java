package com.xnelo.filearch.common.model;

import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DownloadData {
  private final String filename;
  private final InputStream data;
}
