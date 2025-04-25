package com.xnelo.filearch.common.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ResourceType;
import java.util.List;
import lombok.Getter;

@Getter
public class ServiceActionResponse<T> {
  private final ResourceType resourceType;
  private final ActionType actionType;
  private final T data;
  private final List<ServiceError> errors;

  public ServiceActionResponse(final ResourceType type, final ActionType actionType, final T data) {
    this.resourceType = type;
    this.actionType = actionType;
    this.data = data;
    this.errors = null;
  }

  public ServiceActionResponse(
      final ResourceType type, final ActionType actionType, final List<ServiceError> errors) {
    this.resourceType = type;
    this.actionType = actionType;
    this.data = null;
    this.errors = errors;
  }

  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }
}
