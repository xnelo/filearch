package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TagContract {
  @JsonProperty("id")
  private final Long id;

  @JsonProperty("owner_id")
  private final Long ownerId;

  @JsonProperty("tag_name")
  private final String tagName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("stats")
  private final TagStatsContract tagStats;
}
