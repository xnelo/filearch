package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.GroupMemberType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GroupContract {
  @JsonProperty("id")
  private final Long id;

  @JsonProperty("owner_user_id")
  private final Long ownerId;

  @JsonProperty("group_name")
  private final String name;

  @JsonProperty("accepted")
  private final boolean accepted;

  @JsonProperty("group_membership_type")
  private final GroupMemberType groupMembershipType;
}
