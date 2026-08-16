package com.xnelo.filearch.restapi.api.exception;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.mapstruct.factory.Mappers;

public class GlobalExceptionHandlers {
  @ServerExceptionMapper
  public Response handleServiceResponseException(ServiceResponseException ex) {
    ServiceResponse<?> a = ex.toServiceResponse();
    ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);
    return contractMapper.toApiResponse(a, null);
  }
}
