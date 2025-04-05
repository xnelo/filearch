package com.xnelo.filearch.restapi.service.storage.impl;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.restapi.api.contracts.UploadFileResource;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.quarkus.arc.DefaultBean;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@DefaultBean
@RequestScoped
public class LocalFileStorageServiceImpl implements StorageService {
  public static final String LOCAL_FILE_SYSTEM_STORAGE_TYPE = "LFS";

  @ConfigProperty(
      name = "filearch.localfilestorage.localstoragebase",
      defaultValue = "./upload_data_storage")
  String localStorageBase;

  @Override
  public String getStorageType() {
    return LOCAL_FILE_SYSTEM_STORAGE_TYPE;
  }

  @Override
  public Uni<ErrorCode> save(UploadFileResource toUpload, String key) {
    Path localStorageLocation = Path.of(localStorageBase, key);
    Log.debugf("Saving uploaded file to local file system at %s", localStorageLocation);
    return Uni.createFrom()
        .item(
            () -> {
              try {
                Log.infof("Creating directory: %s", localStorageLocation.getParent());
                Files.createDirectories(localStorageLocation.getParent());
              } catch (IOException e) {
                Log.errorf(e, "Unable to create directory %s", localStorageLocation.getParent());
                return ErrorCode.UNABLE_TO_CREATE_DIR;
              }

              try {
                Log.infof(
                    "Copying temp file to permanent storage: original=%s destination=%s",
                    toUpload.file.uploadedFile(), localStorageLocation);
                Files.copy(toUpload.file.uploadedFile(), localStorageLocation);
                return ErrorCode.OK;
              } catch (IOException e2) {
                Log.errorf(
                    e2,
                    "Unable to save file: original=%s destination=%s",
                    toUpload.file.uploadedFile(),
                    localStorageLocation);
                return ErrorCode.UNABLE_TO_SAVE_FILE;
              }
            });
  }
}
