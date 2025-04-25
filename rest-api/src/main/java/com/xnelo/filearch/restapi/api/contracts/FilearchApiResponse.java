package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FilearchApiResponse<T> {
  @JsonProperty("action_responses")
  private final List<FilearchApiActionResponse<T>> actionResponses;
}
