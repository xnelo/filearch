package com.xnelo.filearch.common.model;

import lombok.Getter;

@Getter
public class GroupFile extends File {
  private final GroupItemType itemType;
  private final String folderIn;
  private final Long folderInId;

  public GroupFile(
      final long id,
      final long ownerId,
      final long folderId,
      final StorageType storageType,
      final String storageKey,
      final String originalFilename,
      final String mimeType,
      final GroupItemType itemType,
      final String folderIn,
      final Long folderInId) {
    super(id, ownerId, folderId, storageType, storageKey, originalFilename, mimeType);
    this.itemType = itemType;
    this.folderIn = folderIn;
    this.folderInId = folderInId;
  }
}
