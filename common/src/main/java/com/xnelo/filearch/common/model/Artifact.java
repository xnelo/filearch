package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class Artifact {
  private long id;
  private long ownerId;
  private long storedFileId;
  private String storageKey;
  private String mimeType;
}
