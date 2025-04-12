package com.xnelo.filearch.restapi.api.mappers;

import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.restapi.api.contracts.FilearchApiErrorResponse;
import com.xnelo.filearch.restapi.api.contracts.FilearchApiResponse;
import com.xnelo.filearch.restapi.api.contracts.UserContract;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ContractMapper {
  UserContract toUserContract(User user);

  // Filearch API mappers
  @Mapping(target = "errorCode", expression = "java( error.getErrorCode().getCode() )")
  FilearchApiErrorResponse toErrorResponse(ServiceError error);

  default <R, S> Response toApiResponse(ServiceResponse<S> toConvert, Function<S, R> howToConvert) {
    FilearchApiResponse<R> apiResponse;
    int finalStatusCode = HttpStatusCodeMapper.INITIAL_STATUS_CODE;
    if (toConvert.hasErrors()) {
      ArrayList<FilearchApiErrorResponse> errors = new ArrayList<>();
      for (ServiceError error : toConvert.getErrors()) {
        errors.add(toErrorResponse(error));
        finalStatusCode =
            HttpStatusCodeMapper.combineStatusCode(finalStatusCode, error.getHttpCode());
      }
      apiResponse = new FilearchApiResponse<>(toConvert.getResourceType(), errors);
    } else {
      R userData = howToConvert.apply(toConvert.getData());
      apiResponse = new FilearchApiResponse<>(toConvert.getResourceType(), userData);
      finalStatusCode = 200;
    }
    return Response.status(finalStatusCode).entity(apiResponse).build();
  }
}
