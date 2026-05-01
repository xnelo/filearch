package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.GroupItemType;

public record GroupItemContract(
    @JsonProperty("item_id") long itemId,
    @JsonProperty("item_type") GroupItemType itemType,
    @JsonProperty("group_id") long groupId) {}
