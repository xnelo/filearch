package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class Tag {
  public static final ResourceType TAG_RESOURCE_TYPE = ResourceType.TAG;

  private long id;
  private long ownerId;
  private String tagName;
}
