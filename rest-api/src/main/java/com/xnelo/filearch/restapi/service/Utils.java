package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import java.util.ArrayList;
import java.util.List;

public class Utils {
  private Utils() {}

  /**
   * If a ServiceResponse item is returned then there was a validation issue and processing should
   * stop. If Null is returned then there is no validation error and processing may continue.
   *
   * @param context The context of the request.
   * @param paginationParameters The parameters to validate.
   */
  public static void validatePaginationParameters(
      final ServiceRequestContext context, final PaginationParameters paginationParameters) {
    if (paginationParameters.getAfter() != null && paginationParameters.getAfter() < 0) {
      throw new ServiceResponseException(
          context, ErrorCode.INVALID_AFTER_VALUE, "After value must be greater than 0.", 400);
    } else if (paginationParameters.getLimit() != null && paginationParameters.getLimit() <= 0) {
      throw new ServiceResponseException(
          context,
          ErrorCode.INVALID_RESPONSE_LIMIT,
          "A return limit of '"
              + paginationParameters.getLimit()
              + "' is invalid. Must be greater than 0",
          400);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> ServiceResponse<T> combineServiceActionResponses(
      List<?> toCombine, Class<T> classType) {
    ArrayList<ServiceActionResponse<T>> combinedResponses = new ArrayList<>();
    for (Object serviceAction : toCombine) {
      if (serviceAction instanceof ServiceActionResponse<?> checkedServiceAction) {
        if (checkedServiceAction.getData() != null
            && !(classType.isInstance(checkedServiceAction.getData()))) {
          throw new RuntimeException(
              "Return type of action was not '"
                  + classType.getName()
                  + "'. This should NEVER HAPPEN.");
        }
        combinedResponses.add((ServiceActionResponse<T>) checkedServiceAction);
      } else {
        throw new RuntimeException(
            "Object returned not of type 'ServiceResponse'. This should NEVER HAPPEN.");
      }
    }
    return new ServiceResponse<>(combinedResponses);
  }

  public static <T> ServiceResponse<T> createServiceErrorResponse(
      final ServiceRequestContext serviceRequestContext,
      final ErrorCode errorCode,
      final String errorMessage,
      final int httpCode) {
    return new ServiceResponse<>(
        createServiceActionErrorResponse(serviceRequestContext, errorCode, errorMessage, httpCode));
  }

  public static <T> ServiceActionResponse<T> createServiceActionErrorResponse(
      final ServiceRequestContext serviceRequestContext,
      final ErrorCode errorCode,
      final String errorMessage,
      final int httpCode) {
    return new ServiceActionResponse<>(
        serviceRequestContext.getResourceType(),
        serviceRequestContext.getActionType(),
        List.of(
            ServiceError.builder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .httpCode(httpCode)
                .build()));
  }
}
