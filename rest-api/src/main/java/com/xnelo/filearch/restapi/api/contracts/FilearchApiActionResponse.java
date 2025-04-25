package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ResourceType;
import java.util.List;
import lombok.Getter;

@Getter
public class FilearchApiActionResponse<T> {
  @JsonProperty("type")
  private final ResourceType resourceType;

  @JsonProperty("action")
  private final ActionType actionType;

  @JsonProperty("data")
  private final T data;

  @JsonProperty("errors")
  private final List<FilearchApiErrorResponse> errors;

  public FilearchApiActionResponse(ResourceType type, ActionType actionType, T data) {
    this.resourceType = type;
    this.actionType = actionType;
    this.data = data;
    this.errors = null;
  }

  public FilearchApiActionResponse(
      ResourceType type, ActionType actionType, List<FilearchApiErrorResponse> errors) {
    this.resourceType = type;
    this.actionType = actionType;
    this.data = null;
    this.errors = errors;
  }
}
