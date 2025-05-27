package com.xnelo.filearch.common.utils;

import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceResponse;
import java.util.ArrayList;

public class ServiceResponseUtils {
  public static <T> ServiceResponse<T> updateErrorAndPassThrough(
      ServiceResponse<?> response, final ResourceType newResourceType) {
    ArrayList<ServiceActionResponse<T>> actionResponses = new ArrayList<>();
    for (ServiceActionResponse<?> actionResponse : response.getActionResponses()) {
      actionResponses.add(
          new ServiceActionResponse<>(
              newResourceType, actionResponse.getActionType(), actionResponse.getErrors()));
    }
    return new ServiceResponse<>(actionResponses);
  }
}
