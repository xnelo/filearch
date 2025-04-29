package com.xnelo.filearch.restapi.service.storage.impl;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.quarkus.arc.DefaultBean;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@DefaultBean
@RequestScoped
public class LocalFileStorageServiceImpl implements StorageService {
  @ConfigProperty(
      name = "filearch.localfilestorage.localstoragebase",
      defaultValue = "./upload_data_storage")
  String localStorageBase;

  @Override
  public StorageType getStorageType() {
    return StorageType.LOCAL_FILE_SYSTEM;
  }

  @Override
  public Uni<ErrorCode> save(final FileUpload toUpload, final String key) {
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
                    toUpload.uploadedFile(), localStorageLocation);
                Files.copy(toUpload.uploadedFile(), localStorageLocation);
                return ErrorCode.OK;
              } catch (IOException e2) {
                Log.errorf(
                    e2,
                    "Unable to save file: original=%s destination=%s",
                    toUpload.uploadedFile(),
                    localStorageLocation);
                return ErrorCode.UNABLE_TO_SAVE_FILE;
              }
            });
  }

  @Override
  public Uni<ErrorCode> delete(final String key) {
    Path localStorageLocation = Path.of(localStorageBase, key);
    return Uni.createFrom()
        .item(
            () -> {
              try {
                Log.infof("Deleting file from Local Storage System: file=%s", localStorageLocation);
                Files.delete(localStorageLocation);
                return ErrorCode.OK;
              } catch (IOException e) {
                Log.errorf(e, "Unable to delete file: file=%s", localStorageLocation);
                return ErrorCode.UNABLE_TO_DELETE_FILE;
              }
            });
  }
}
