package com.xnelo.filearch.common.service;

import com.xnelo.filearch.common.model.ResourceType;
import java.util.List;
import lombok.Getter;

@Getter
public class ServiceResponse<T> {
  private final ResourceType resourceType;
  private final T data;
  private final List<ServiceError> errors;

  public ServiceResponse(final ResourceType type, final T data) {
    this.resourceType = type;
    this.data = data;
    this.errors = null;
  }

  public ServiceResponse(final ResourceType type, final List<ServiceError> errors) {
    this.resourceType = type;
    this.data = null;
    this.errors = errors;
  }

  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }
}
