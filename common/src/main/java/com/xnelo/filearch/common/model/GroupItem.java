package com.xnelo.filearch.common.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GroupItem {
  private long itemId;
  private GroupItemType itemType;
  private long groupId;
}
