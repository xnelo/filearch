package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PaginationContract<T> {
  @JsonProperty("data")
  private final List<T> data;

  @JsonProperty("has_next")
  private final boolean hasNext;
}
