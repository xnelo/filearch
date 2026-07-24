package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TagShareContract {
  @JsonProperty("tag_id")
  private final Long tagId;

  @JsonProperty("group_id")
  private final Long groupId;
}
