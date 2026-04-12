package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class GroupMemberPermission {
  private long userId;
  private long groupId;
  private GroupPermissionType permission;
}
