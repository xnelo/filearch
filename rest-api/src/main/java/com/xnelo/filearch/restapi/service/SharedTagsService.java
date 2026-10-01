package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.restapi.data.SharedTagsRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class SharedTagsService {
  @Inject SharedTagsRepo sharedTagsRepo;

  public Uni<ServiceRequestContext> checkTagShareNotExists(
      ServiceRequestContext requestContext, final long tagId, final long groupId) {
    return sharedTagsRepo
        .tagShareExists(tagId, groupId)
        .map(
            res -> {
              if (res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.TAG_SHARE_DOES_NOT_EXIST,
                    "Shared tag (" + tagId + ") exists.",
                    400);
              }

              return requestContext;
            });
  }

  public Uni<ServiceRequestContext> checkTagShareExists(
      ServiceRequestContext requestContext, final long tagId) {
    Utils.checkGroupInRequest(requestContext);

    return checkTagShareExists(requestContext, tagId, requestContext.getGroupId());
  }

  public Uni<ServiceRequestContext> checkTagShareExists(
      ServiceRequestContext requestContext, final long tagId, final long groupId) {
    return internalCheckTagShareExists(requestContext, tagId, groupId);
  }

  private Uni<ServiceRequestContext> internalCheckTagShareExists(
      ServiceRequestContext requestContext, final long tagId, final long groupId) {
    return sharedTagsRepo
        .tagShareExists(tagId, groupId)
        .map(
            res -> {
              if (!res) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.TAG_SHARE_DOES_NOT_EXIST,
                    "Shared tag (" + tagId + ") does not exist.",
                    404);
              }

              return requestContext;
            });
  }
}
