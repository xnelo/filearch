package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class FilearchApiErrorResponse {
  @JsonProperty("error_code")
  private final int errorCode;

  @JsonProperty("error_message")
  private final String errorMessage;

  @JsonProperty("http_code")
  private final int httpCode;
}
