package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class File {
  public static final ResourceType FILE_RESOURCE_TYPE = ResourceType.FILE;

  private long id;
  private long ownerId;
  private StorageType storageType;
  private String storageKey;
}
