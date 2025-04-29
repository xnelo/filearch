package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.data.SequenceRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@RequestScoped
public class FileService {
  @Inject UserService userService;
  @Inject SequenceRepo sequenceRepo;
  @Inject StorageService storageService;
  @Inject StoredFilesRepo storedFilesRepo;

  public Uni<ServiceResponse<File>> uploadFiles(
      final FileUploadContract toUpload, final UserToken uploadingUser) {
    return userService
        .getUserFromUserToken(uploadingUser)
        .chain(
            userResponse -> {
              User user = userResponse.getActionResponses().getFirst().getData();
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                File.FILE_RESOURCE_TYPE,
                                ActionType.UPLOAD,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage(
                                            "Cannot upload file because user does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              ArrayList<Uni<ServiceActionResponse<File>>> uploadResults = new ArrayList<>();
              for (final FileUpload fileToUpload : toUpload.files) {
                uploadResults.add(uploadIndividualFile(user, toUpload.location, fileToUpload));
              }
              return Uni.combine()
                  .all()
                  .unis(uploadResults)
                  .with(FileService::combineFileUploadUnis);
            });
  }

  @SuppressWarnings("unchecked")
  static ServiceResponse<File> combineFileUploadUnis(List<?> toCombine) {
    ArrayList<ServiceActionResponse<File>> combinedResponses = new ArrayList<>();
    for (Object serviceAction : toCombine) {
      if (serviceAction instanceof ServiceActionResponse<?> checkedServiceAction) {
        if (!(checkedServiceAction.getData() instanceof File)) {
          throw new RuntimeException(
              "Return type of action was not 'File'. This should NEVER HAPPEN.");
        }
        combinedResponses.add((ServiceActionResponse<File>) checkedServiceAction);
      } else {
        throw new RuntimeException(
            "Object returned not of type 'ServiceResponse'. This should NEVER HAPPEN.");
      }
    }
    return new ServiceResponse<>(combinedResponses);
  }

  Uni<ServiceActionResponse<File>> uploadIndividualFile(
      final User user, final String location, final FileUpload fileToUpload) {
    return createStorageKey(user, fileToUpload.fileName())
        .chain(
            uploadKey ->
                storageService
                    .save(fileToUpload, uploadKey)
                    .chain(
                        errorCode -> {
                          if (errorCode != ErrorCode.OK) {
                            return Uni.createFrom()
                                .item(
                                    new ServiceActionResponse<>(
                                        ResourceType.FILE,
                                        ActionType.UPLOAD,
                                        List.of(
                                            ServiceError.builder()
                                                .httpCode(400)
                                                .errorMessage(
                                                    "Unable to store file '"
                                                        + fileToUpload.fileName()
                                                        + "'")
                                                .errorCode(errorCode)
                                                .build())));
                          } else {
                            return storedFilesRepo
                                .createStoredFile(
                                    user.getId(), storageService.getStorageType(), uploadKey)
                                .map(
                                    dbFile ->
                                        new ServiceActionResponse<>(
                                            ResourceType.FILE, ActionType.UPLOAD, dbFile));
                          }
                        }));
  }

  Uni<String> createStorageKey(final User user, final String filename) {
    return sequenceRepo
        .getNextFileUploadNumber()
        .map(fileUploadNumber -> user.getId() + "/" + fileUploadNumber + "_" + filename);
  }
}
