package com.xnelo.filearch.common.service;

import com.xnelo.filearch.common.model.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ServiceError {
  private ErrorCode errorCode;
  private String errorMessage;
  private int httpCode;
}
