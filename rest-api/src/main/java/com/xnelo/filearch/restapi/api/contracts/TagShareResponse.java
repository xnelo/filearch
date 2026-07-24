package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TagShareResponse {
  @JsonProperty("tag_id")
  private final Long tagId;

  @JsonProperty("group_id")
  private final Long groupId;

  @JsonProperty("action_successful")
  private final boolean actionSuccessful;
}
