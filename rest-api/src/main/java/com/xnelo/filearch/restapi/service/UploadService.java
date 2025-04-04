package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.user.User;
import com.xnelo.filearch.restapi.api.contracts.UploadFileResource;
import com.xnelo.filearch.restapi.api.contracts.UploadResponse;
import com.xnelo.filearch.restapi.data.SequenceRepo;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class UploadService {
  @Inject StorageService storageService;
  @Inject SequenceRepo sequenceRepo;

  public Uni<UploadResponse> uploadFile(UploadFileResource toUpload, User user) {
    return createStorageKey(user, toUpload)
        .chain(uploadKey -> storageService.save(toUpload, uploadKey))
        .map(UploadService::toUploadResponse);
  }

  Uni<String> createStorageKey(final User user, final UploadFileResource toUpload) {
    return sequenceRepo
        .getNextFileUploadNumber()
        .map(
            fileUploadNumber ->
                user.getId() + "/" + fileUploadNumber + "_" + toUpload.file.fileName());
  }

  static UploadResponse toUploadResponse(ErrorCode result) {
    if (result == ErrorCode.OK) {
      return UploadResponse.SUCCESS;
    } else {
      return UploadResponse.builder()
          .success(false)
          .errorCode(result)
          .errorMessage("Error storing file.")
          .build();
    }
  }
}
