package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.user.User;
import com.xnelo.filearch.restapi.api.contracts.UploadFileResource;
import com.xnelo.filearch.restapi.api.contracts.UploadResponse;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class UploadService {
  @Inject StorageService storageService;

  public Uni<UploadResponse> uploadFile(UploadFileResource toUpload, User user) {
    // create the key to save the file to its final location

    return storageService
        .save()
        .map(
            storeResult -> {
              if (storeResult != ErrorCode.OK) {
                return UploadResponse.builder()
                    .success(false)
                    .errorCode(storeResult)
                    .errorMessage("Error storing file.")
                    .build();
              } else {
                return UploadResponse.SUCCESS;
              }
            });
  }
}
