package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TagShareBulkContract {
  @JsonProperty("share")
  private final List<TagShareContract> tagsToShare;
}
