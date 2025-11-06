package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.utils.ServiceResponseUtils;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.config.FilearchConfig;
import com.xnelo.filearch.restapi.data.SequenceRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import com.xnelo.filearch.restapi.service.folder.FolderService;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class FileService {
  @Inject UserService userService;
  @Inject SequenceRepo sequenceRepo;
  @Inject StorageService storageService;
  @Inject StoredFilesRepo storedFilesRepo;
  @Inject FolderService folderService;
  @Inject FilearchConfig config;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFiles(
      final UserToken userInfo,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    if (after != null && after < 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.GET,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_AFTER_VALUE)
                              .errorMessage("After value must be greater than 0.")
                              .httpCode(400)
                              .build()))));
    }

    if (limit != null && limit <= 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.GET,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_RESPONSE_LIMIT)
                              .errorMessage(
                                  "A return limit of '"
                                      + limit
                                      + "' is invalid. Must be greater than 0")
                              .httpCode(400)
                              .build()))));
    }

    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              User user = userResponse.getActionResponses().getFirst().getData();
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FILE,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage("User does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return storedFilesRepo
                  .getAll(user.getId(), after, limit, sortDirection)
                  .map(
                      paginatedFiles ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FILE,
                                  ActionType.GET,
                                  paginationMapper.toPaginatedResponse(paginatedFiles))));
            });
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFilesInFolder(
      final UserToken userInfo,
      final Long folderId,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    if (after != null && after < 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.GET,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_AFTER_VALUE)
                              .errorMessage("After value must be greater than 0.")
                              .httpCode(400)
                              .build()))));
    }

    if (limit != null && limit <= 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.GET,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_RESPONSE_LIMIT)
                              .errorMessage(
                                  "A return limit of '"
                                      + limit
                                      + "' is invalid. Must be greater than 0")
                              .httpCode(400)
                              .build()))));
    }

    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              User user = userResponse.getActionResponses().getFirst().getData();
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FILE,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage("User does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return storedFilesRepo
                  .getFilesInFolder(folderId, user.getId(), after, limit, sortDirection)
                  .map(
                      paginatedFiles ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FILE,
                                  ActionType.GET,
                                  paginationMapper.toPaginatedResponse(paginatedFiles))));
            });
  }

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

              if (toUpload.folderId != null) {
                return folderService
                    .getFolderById(toUpload.folderId, user.getId())
                    .chain(
                        folderServiceResponse -> {
                          Folder folder =
                              folderServiceResponse.getActionResponses().getFirst().getData();
                          if (folder == null) {
                            return Uni.createFrom()
                                .item(
                                    new ServiceResponse<>(
                                        new ServiceActionResponse<>(
                                            ResourceType.FILE,
                                            ActionType.UPLOAD,
                                            List.of(
                                                ServiceError.builder()
                                                    .errorCode(ErrorCode.FOLDER_DOES_NOT_EXIST)
                                                    .errorMessage(
                                                        "Desired upload folder does not exist.")
                                                    .httpCode(404)
                                                    .build()))));
                          }
                          return uploadAllFiles(toUpload.files, user, folder.getId());
                        });
              } else {
                return uploadAllFiles(toUpload.files, user, user.getRootFolderId());
              }
            });
  }

  Uni<ServiceResponse<File>> uploadAllFiles(
      final List<FileUpload> files, final User user, final long folderId) {
    ArrayList<Uni<ServiceActionResponse<File>>> uploadResults = new ArrayList<>();

    for (final FileUpload fileToUpload : files) {
      uploadResults.add(uploadIndividualFile(user, folderId, fileToUpload));
    }
    return Uni.combine().all().unis(uploadResults).with(FileService::combineFileActionUnis);
  }

  @SuppressWarnings("unchecked")
  static ServiceResponse<File> combineFileActionUnis(List<?> toCombine) {
    ArrayList<ServiceActionResponse<File>> combinedResponses = new ArrayList<>();
    for (Object serviceAction : toCombine) {
      if (serviceAction instanceof ServiceActionResponse<?> checkedServiceAction) {
        if (checkedServiceAction.getData() != null
            && !(checkedServiceAction.getData() instanceof File)) {
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
      final User user, final Long folderId, final FileUpload fileToUpload) {
    return createStorageKey(user)
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
                                    user.getId(),
                                    folderId,
                                    storageService.getStorageType(),
                                    uploadKey,
                                    fileToUpload.fileName())
                                .map(
                                    dbFile ->
                                        new ServiceActionResponse<>(
                                            ResourceType.FILE, ActionType.UPLOAD, dbFile));
                          }
                        }));
  }

  Uni<String> createStorageKey(final User user) {
    return sequenceRepo
        .getNextFileUploadNumber()
        .map(fileUploadNumber -> user.getId() + "/" + fileUploadNumber);
  }

  public Uni<ServiceResponse<File>> getFileMetadata(final long fileId, final UserToken userInfo) {
    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              User user = userResponse.getActionResponses().getFirst().getData();
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FILE,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage(
                                            "Cannot retrieve file because user does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return storedFilesRepo
                  .getStoredFile(fileId, user.getId())
                  .chain(
                      file -> {
                        if (file == null) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          ResourceType.FILE,
                                          ActionType.GET,
                                          List.of(
                                              ServiceError.builder()
                                                  .errorCode(ErrorCode.FILE_DOES_NOT_EXIST)
                                                  .errorMessage("File was not found.")
                                                  .httpCode(404)
                                                  .build()))));
                        }

                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.FILE, ActionType.GET, file)));
                      });
            });
  }

  @SuppressWarnings("unchecked")
  public Uni<ServiceResponse<File>> deleteFile(final long fileId, final UserToken userInfo) {
    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              if (userResponse.hasError()) {
                return Uni.createFrom()
                    .item(
                        ServiceResponseUtils.updateErrorAndPassThrough(
                            userResponse, ResourceType.FILE));
              }
              User user = userResponse.getActionResponses().getFirst().getData();
              return deleteIndividualFile(fileId, user.getId()).map(ServiceResponse::new);
            });
  }

  private Uni<ServiceActionResponse<File>> deleteIndividualFile(
      final long fileId, final long userId) {
    return storedFilesRepo
        .getStoredFile(fileId, userId)
        .chain(
            file -> {
              if (file == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceActionResponse<>(
                            ResourceType.FILE,
                            ActionType.DELETE,
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.FILE_DOES_NOT_EXIST)
                                    .errorMessage("File does not exist. fileId=" + fileId)
                                    .httpCode(404)
                                    .build())));
              }

              return storageService
                  .delete(file.getStorageKey())
                  .chain(
                      storageDeleteResult -> {
                        if (storageDeleteResult != ErrorCode.OK) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceActionResponse<>(
                                      ResourceType.FILE,
                                      ActionType.DELETE,
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(storageDeleteResult)
                                              .errorMessage("Error deleting file.")
                                              .httpCode(500)
                                              .build())));
                        }

                        return storedFilesRepo
                            .deleteStoredFile(fileId, userId)
                            .map(
                                deleteSuccessful -> {
                                  if (!deleteSuccessful) {
                                    return new ServiceActionResponse<>(
                                        ResourceType.FILE,
                                        ActionType.DELETE,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.UNABLE_TO_DELETE_FILE)
                                                .errorMessage("Unable to delete file")
                                                .httpCode(400)
                                                .build()));
                                  }

                                  return new ServiceActionResponse<>(
                                      ResourceType.FILE, ActionType.DELETE, file);
                                });
                      });
            });
  }

  public Uni<ServiceResponse<DownloadData>> getFileForDownload(
      final long fileId, final UserToken userInfo) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.FILE,
        ActionType.DOWNLOAD,
        user ->
            storedFilesRepo
                .getStoredFile(fileId, user.getId())
                .chain(
                    fileMetadata -> {
                      if (fileMetadata == null) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.FILE,
                                        ActionType.DOWNLOAD,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.FILE_DOES_NOT_EXIST)
                                                .errorMessage("File does not exist")
                                                .httpCode(404)
                                                .build()))));
                      }

                      try {
                        return storageService
                            .getFileData(fileMetadata.getStorageKey())
                            .map(
                                fileDataStream ->
                                    new ServiceResponse<>(
                                        new ServiceActionResponse<>(
                                            ResourceType.FILE,
                                            ActionType.DOWNLOAD,
                                            new DownloadData(
                                                fileMetadata.getOriginalFilename(),
                                                fileDataStream))));
                      } catch (IOException e) {
                        Log.errorf(
                            e,
                            "Exception encountered while opening file inputstream. fileId:%d fileStorageKey:%s",
                            fileMetadata.getId(),
                            fileMetadata.getStorageKey());
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        ResourceType.FILE,
                                        ActionType.DOWNLOAD,
                                        List.of(
                                            ServiceError.builder()
                                                .errorCode(ErrorCode.UNABLE_TO_OPEN_INPUT_STREAM)
                                                .errorMessage(
                                                    "Unable to open Input Stream for file.")
                                                .httpCode(500)
                                                .build()))));
                      }
                    }));
  }

  public Uni<List<File>> getFilesInFoldersInternal(final List<Long> folderIds, final long userId) {
    return storedFilesRepo.getFilesInFolders(folderIds, userId);
  }

  public Uni<ServiceResponse<File>> bulkDeleteFiles(
      final List<Long> filesIdsToDelete, final UserToken userInfo) {
    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              if (userResponse.hasError()) {
                return Uni.createFrom()
                    .item(
                        ServiceResponseUtils.updateErrorAndPassThrough(
                            userResponse, ResourceType.FILE));
              }
              User user = userResponse.getActionResponses().getFirst().getData();
              return bulkDeleteFiles(filesIdsToDelete, user.getId());
            });
  }

  public Uni<ServiceResponse<File>> bulkDeleteFiles(
      final List<Long> filesIdsToDelete, final long userId) {
    if (filesIdsToDelete.size() > config.bulkActions().maxDelete()) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.DELETE,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.TOO_MANY_BULK_OPERATIONS)
                              .errorMessage(
                                  "Maximum number of bulk operations for bulk delete is "
                                      + config.bulkActions().maxDelete()
                                      + ". "
                                      + filesIdsToDelete.size()
                                      + " were passed in.")
                              .httpCode(400)
                              .build()))));
    }

    ArrayList<Uni<ServiceActionResponse<File>>> fileDeleteUnis =
        new ArrayList<>(filesIdsToDelete.size());
    filesIdsToDelete.forEach(
        fileIdToDelete -> fileDeleteUnis.add(deleteIndividualFile(fileIdToDelete, userId)));
    return Uni.combine().all().unis(fileDeleteUnis).with(FileService::combineFileActionUnis);
  }
}
