package com.xnelo.filearch.common.service;

import java.util.List;
import lombok.Getter;

@Getter
public class ServiceResponse<T> {
  private final List<ServiceActionResponse<T>> actionResponses;

  @SafeVarargs
  public ServiceResponse(ServiceActionResponse<T>... actionResponses) {
    this.actionResponses = List.of(actionResponses);
  }

  public ServiceResponse(List<ServiceActionResponse<T>> actionResponses) {
    this.actionResponses = actionResponses;
  }
}
