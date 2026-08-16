package com.xnelo.filearch.common.exception;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import java.util.List;

public class ServiceResponseException extends RuntimeException {
  private final ServiceRequestContext requestContext;
  private final ErrorCode errorCode;
  private final int httpStatus;

  public ServiceResponseException(
      final ServiceRequestContext requestContext,
      final ErrorCode errorCode,
      final String errorMessage,
      final int httpStatus) {
    super(errorMessage);
    this.requestContext = requestContext;
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  public <T> ServiceResponse<T> toServiceResponse() {
    return new ServiceResponse<>(
        new ServiceActionResponse<>(
            requestContext.getResourceType(),
            requestContext.getActionType(),
            List.of(
                ServiceError.builder()
                    .errorCode(errorCode)
                    .errorMessage(this.getMessage())
                    .httpCode(httpStatus)
                    .build())));
  }
}
