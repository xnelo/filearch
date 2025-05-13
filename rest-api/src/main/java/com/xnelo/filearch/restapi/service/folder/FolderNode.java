package com.xnelo.filearch.restapi.service.folder;

import com.xnelo.filearch.common.model.Folder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class FolderNode {
  private final Folder data;
  FolderNode parent;
  @Getter List<FolderNode> children = new ArrayList<>();

  private FolderNode(final Folder data) {
    this.data = data;
  }

  public static FolderNode of(final Folder data) {
    return new FolderNode(data);
  }

  public Long getParentId() {
    return data.getParentId();
  }

  public boolean isRoot() {
    return data.getParentId() == null;
  }

  void addChildNode(final FolderNode newChild) {
    children.add(newChild);
    newChild.setParent(this);
  }

  void setParent(final FolderNode parentNode) {
    this.parent = parentNode;
  }

  public long getFolderId() {
    return this.data.getId();
  }
}
