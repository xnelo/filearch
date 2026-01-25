package com.xnelo.filearch.restapi.api.contracts;

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
}
