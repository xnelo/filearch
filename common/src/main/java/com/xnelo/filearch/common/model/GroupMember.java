package com.xnelo.filearch.common.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GroupMember {
  private final long userId;
  private final String username;
  private final long groupId;
  private final boolean accepted;
  private final GroupMemberType memberType;
}
