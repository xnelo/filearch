package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.GroupMemberType;

public record GroupMemberContract(
    @JsonProperty("user_id") long userId,
    @JsonProperty("username") String username,
    @JsonProperty("group_id") long groupId,
    @JsonProperty("accepted") boolean accepted,
    @JsonProperty("group_membership_type") GroupMemberType memberType) {}
