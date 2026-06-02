package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.GroupPermissionType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GroupMemberAllPermissionsContract {
  private @JsonProperty("user_id") long userId;
  private @JsonProperty("group_id") long groupId;
  private @JsonProperty("permissions") List<GroupPermissionType> permissions;
}
