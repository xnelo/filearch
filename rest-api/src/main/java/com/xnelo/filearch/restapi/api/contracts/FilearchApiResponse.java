package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.ResourceType;
import java.util.List;
import lombok.Getter;

@Getter
public class FilearchApiResponse<T> {
  @JsonProperty("type")
  private final ResourceType resourceType;

  @JsonProperty("data")
  private final T data;

  @JsonProperty("errors")
  private final List<FilearchApiErrorResponse> errors;

  public FilearchApiResponse(ResourceType type, T data) {
    this.resourceType = type;
    this.data = data;
    this.errors = null;
  }

  public FilearchApiResponse(ResourceType type, List<FilearchApiErrorResponse> errors) {
    this.resourceType = type;
    this.data = null;
    this.errors = errors;
  }
}
