package com.xnelo.filearch.common.service.storage.impl;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.common.service.storage.StorageService;
import io.quarkus.arc.DefaultBean;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@DefaultBean
@ApplicationScoped
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
  public Uni<ErrorCode> save(final byte[] toUpload, final String key) {
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
                    "Saving byte array to permanent storage: destination=%s", localStorageLocation);
                Files.write(localStorageLocation, toUpload);
                return ErrorCode.OK;
              } catch (IOException e2) {
                Log.errorf(
                    e2, "Unable to save byte array to file: destination=%s", localStorageLocation);
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

  @Override
  public Uni<ErrorCode> bulkDelete(final List<String> keys) {
    if (keys.isEmpty()) {
      return Uni.createFrom().item(ErrorCode.OK);
    }

    List<Uni<ErrorCode>> individualDeletes = keys.stream().map(this::delete).toList();

    return Uni.combine()
        .all()
        .unis(individualDeletes)
        .with(ErrorCode.class, LocalFileStorageServiceImpl::combineErrorCodes);
  }

  static ErrorCode combineErrorCodes(List<ErrorCode> toCombine) {
    ErrorCode finalErrorCode = ErrorCode.OK;
    for (ErrorCode code : toCombine) {
      if (code.getCode() > finalErrorCode.getCode()) {
        finalErrorCode = code;
      }
    }
    return finalErrorCode;
  }

  @Override
  public Uni<InputStream> getFileData(final String key) {
    Path localStorageLocation = Path.of(localStorageBase, key);
    Log.infof("Getting input stream for file. file=%s", localStorageLocation);
    return Uni.createFrom()
        .item(
            () -> {
              if (!Files.exists(localStorageLocation)) {
                Log.infof("File doesn't exist. file=%s", localStorageLocation);
                return null;
              } else {
                try {
                  return Files.newInputStream(localStorageLocation);
                } catch (IOException e) {
                  Log.errorf(e, "Error retrieving file. file=%s", localStorageLocation);
                  return null;
                }
              }
            });
  }
}
