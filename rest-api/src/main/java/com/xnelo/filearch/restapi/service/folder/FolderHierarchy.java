package com.xnelo.filearch.restapi.service.folder;

import com.xnelo.filearch.common.model.Folder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public class FolderHierarchy {
  private final Map<Long, FolderNode> folderNodes;
  @Getter private final FolderNode rootNode;

  private FolderHierarchy(final Collection<Folder> folders) {
    FolderNode tmpRootNode = null;
    folderNodes = new HashMap<>(folders.size());

    for (Folder folder : folders) {
      FolderNode newNode = FolderNode.of(folder);
      folderNodes.put(folder.getId(), newNode);
      if (newNode.isRoot()) {
        if (tmpRootNode != null) {
          throw new IllegalStateException("Found a second Root node in hierarchy.");
        } else {
          tmpRootNode = newNode;
        }
      }
    }

    rootNode = tmpRootNode;
    if (rootNode == null) {
      throw new IllegalStateException("No root node found in hierarchy.");
    }

    folderNodes.forEach(
        (key, value) -> {
          if (value.getParentId() != null) {
            FolderNode parentNode = folderNodes.get(value.getParentId());
            parentNode.addChildNode(value);
          } // else do nothing this is the root
        });
  }

  public static FolderHierarchy of(final Collection<Folder> folders) {
    return new FolderHierarchy(folders);
  }

  public FolderNode getNodeFromId(final long folderId) {
    return this.folderNodes.get(folderId);
  }
}
