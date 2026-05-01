package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.GroupPermissionType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GroupMemberPermissionContract {
  @JsonProperty("user_id")
  private long userId;

  @JsonProperty("group_id")
  private final long groupId;

  @JsonProperty("permission")
  private final GroupPermissionType permission;
}
