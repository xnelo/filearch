package com.xnelo.filearch.restapi.api.mappers;

import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.Folder;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.restapi.api.contracts.*;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ContractMapper {
  UserContract toUserContract(User user);

  FileContract toFileContract(File file);

  FolderContract toFolderContract(Folder folder);

  // Filearch API mappers
  @Mapping(target = "errorCode", expression = "java( error.getErrorCode().getCode() )")
  FilearchApiErrorResponse toErrorResponse(ServiceError error);

  default <R, S> FilearchApiActionResponse<R> toApiActionResponse(
      ServiceActionResponse<S> toConvert, Function<S, R> howToConvert) {
    FilearchApiActionResponse<R> apiResponse;
    if (toConvert.hasErrors()) {
      ArrayList<FilearchApiErrorResponse> errors = new ArrayList<>();
      for (ServiceError error : toConvert.getErrors()) {
        errors.add(toErrorResponse(error));
      }
      apiResponse =
          new FilearchApiActionResponse<>(
              toConvert.getResourceType(), toConvert.getActionType(), errors);
    } else {
      R userData = howToConvert.apply(toConvert.getData());
      apiResponse =
          new FilearchApiActionResponse<>(
              toConvert.getResourceType(), toConvert.getActionType(), userData);
    }
    return apiResponse;
  }

  default <R, S> Response toApiResponse(ServiceResponse<S> toConvert, Function<S, R> howToConvert) {
    List<FilearchApiActionResponse<R>> convertedActions =
        new ArrayList<>(toConvert.getActionResponses().size());
    int finalStatusCode = HttpStatusCodeMapper.INITIAL_STATUS_CODE;
    for (ServiceActionResponse<S> action : toConvert.getActionResponses()) {
      convertedActions.add(toApiActionResponse(action, howToConvert));
      if (!action.hasErrors()) {
        finalStatusCode = HttpStatusCodeMapper.combineStatusCode(finalStatusCode, 200);
      } else {
        for (ServiceError error : action.getErrors()) {
          finalStatusCode =
              HttpStatusCodeMapper.combineStatusCode(finalStatusCode, error.getHttpCode());
        }
      }
    }
    return Response.status(finalStatusCode)
        .entity(new FilearchApiResponse<>(convertedActions))
        .build();
  }
}
