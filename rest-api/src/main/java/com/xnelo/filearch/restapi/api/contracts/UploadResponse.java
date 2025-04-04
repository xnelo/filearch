package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UploadResponse {
  public static UploadResponse SUCCESS = new UploadResponse(true, ErrorCode.OK, null);

  @JsonProperty("success")
  final boolean success;

  @JsonProperty("error_code")
  final ErrorCode errorCode;

  @JsonProperty("error_message")
  final String errorMessage;
}
