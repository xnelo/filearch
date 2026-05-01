package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class Group {
  public static final ResourceType GROUP_RESOURCE_TYPE = ResourceType.GROUP;

  private long id;
  private long ownerId;
  private String name;
}
