package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class Folder {
  public static final ResourceType FOLDER_RESOURCE_TYPE = ResourceType.FOLDER;

  private long id;
  private long ownerId;
  private Long parentId;
  private String folderName;

  public boolean isRootFolder() {
    return parentId == null;
  }
}
