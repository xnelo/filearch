package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TagStatsContract {
  @JsonProperty("usage_count")
  private final Long usageCount;

  @JsonProperty("groups_in_count")
  private final Long groupsInCount;
}
