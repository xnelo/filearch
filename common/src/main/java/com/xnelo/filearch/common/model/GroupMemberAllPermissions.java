package com.xnelo.filearch.common.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class GroupMemberAllPermissions {
  private long userId;
  private long groupId;
  private List<GroupPermissionType> permissions;
}
