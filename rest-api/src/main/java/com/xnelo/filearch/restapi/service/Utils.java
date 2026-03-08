package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import java.util.ArrayList;
import java.util.List;

public class Utils {
  private Utils() {}

  public static <T> ServiceResponse<T> validatePaginationParameters(
      final PaginationParameters paginationParameters,
      final ResourceType resourceType,
      final ActionType actionType) {
    if (paginationParameters.getAfter() != null && paginationParameters.getAfter() < 0) {
      return new ServiceResponse<>(
          new ServiceActionResponse<>(
              resourceType,
              actionType,
              List.of(
                  ServiceError.builder()
                      .errorCode(ErrorCode.INVALID_AFTER_VALUE)
                      .errorMessage("After value must be greater than 0.")
                      .httpCode(400)
                      .build())));
    } else if (paginationParameters.getLimit() != null && paginationParameters.getLimit() <= 0) {
      return new ServiceResponse<>(
          new ServiceActionResponse<>(
              resourceType,
              actionType,
              List.of(
                  ServiceError.builder()
                      .errorCode(ErrorCode.INVALID_RESPONSE_LIMIT)
                      .errorMessage(
                          "A return limit of '"
                              + paginationParameters.getLimit()
                              + "' is invalid. Must be greater than 0")
                      .httpCode(400)
                      .build())));
    } else {
      return null;
    }
  }

  /**
   * Update a service response with new resource or action type.
   *
   * @param response The original response to update.
   * @param newResourceType If present this will be the ResourceType after updating the response
   *     message. If null the original response's resource type is used.
   * @param newActionType If present this will be the ActionType after updating the response
   *     message. If null the original response's action type is used.
   * @return The updated response.
   * @param <T> The type of data this response represents.
   */
  public static <T> ServiceResponse<T> updateErrorAndPassThrough(
      ServiceResponse<?> response, ResourceType newResourceType, ActionType newActionType) {
    ArrayList<ServiceActionResponse<T>> actionResponses = new ArrayList<>();
    for (ServiceActionResponse<?> actionResponse : response.getActionResponses()) {
      actionResponses.add(
          new ServiceActionResponse<>(
              newResourceType == null ? actionResponse.getResourceType() : newResourceType,
              newActionType == null ? actionResponse.getActionType() : newActionType,
              actionResponse.getErrors()));
    }
    return new ServiceResponse<>(actionResponses);
  }
}
