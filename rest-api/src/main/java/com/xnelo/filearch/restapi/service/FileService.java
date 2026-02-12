package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.data.ArtifactRepo;
import com.xnelo.filearch.common.json.JsonUtil;
import com.xnelo.filearch.common.messaging.MessagingMapper;
import com.xnelo.filearch.common.messaging.ProcessFileRequest;
import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.utils.ServiceResponseUtils;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.config.FilearchConfig;
import com.xnelo.filearch.restapi.data.FileTagsRepo;
import com.xnelo.filearch.restapi.data.SequenceRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import com.xnelo.filearch.restapi.service.folder.FolderService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class FileService {
  @Inject UserService userService;
  @Inject SequenceRepo sequenceRepo;
  @Inject StorageService storageService;
  @Inject StoredFilesRepo storedFilesRepo;
  @Inject ArtifactRepo artifactRepo;
  @Inject FolderService folderService;
  @Inject FileTagsRepo fileTagsRepo;
  @Inject FilearchConfig config;
  @Inject TagService tagService;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);
  final MessagingMapper messagingMapper = Mappers.getMapper(MessagingMapper.class);

  @Channel("file-proc-requests")
  Emitter<String> fileProcRequestEmitter;

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFiles(
      final UserToken userInfo, final PaginationParameters paginationParameters) {
    if (paginationParameters.getAfter() != null && paginationParameters.getAfter() < 0) {
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

    if (paginationParameters.getLimit() != null && paginationParameters.getLimit() <= 0) {
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
                                      + paginationParameters.getLimit()
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
                  .getAll(user.getId(), paginationParameters)
                  .map(
                      paginatedFiles ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FILE,
                                  ActionType.GET,
                                  paginationMapper.toPaginatedResponse(paginatedFiles))));
            });
  }

  public Uni<ServiceResponse<List<Long>>> getAllFileIdsInFolder(
      final UserToken userToken, final Long folderId) {
    return userService
        .getUserFromUserToken(userToken)
        .chain(
            userResponse -> {
              User user = userResponse.getActionResponses().getFirst().getData();
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FILE_IDS,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage("User does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return storedFilesRepo
                  .getFileIdsInFolder(user.getId(), folderId)
                  .map(
                      fileIds ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FILE_IDS, ActionType.GET, fileIds)));
            });
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFilesInFolder(
      final UserToken userInfo,
      final Long folderId,
      final PaginationParameters paginationParameters) {
    if (paginationParameters.getAfter() != null && paginationParameters.getAfter() < 0) {
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

    if (paginationParameters.getLimit() != null && paginationParameters.getLimit() <= 0) {
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
                                      + paginationParameters.getLimit()
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
                  .getFilesInFolder(folderId, user.getId(), paginationParameters)
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
                                    fileToUpload.fileName(),
                                    fileToUpload.contentType())
                                .invoke(this::sendProcessRequest)
                                .map(
                                    dbFile ->
                                        new ServiceActionResponse<>(
                                            ResourceType.FILE, ActionType.UPLOAD, dbFile));
                          }
                        }));
  }

  void sendProcessRequest(File dbFile) {
    ProcessFileRequest request = messagingMapper.toFileRequest(dbFile);
    fileProcRequestEmitter.send(JsonUtil.toJsonString(request));
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

              return fileTagsRepo
                  .deleteAllFileMappings(file.getId())
                  .chain(
                      deleteMappingsSuccess -> {
                        if (!deleteMappingsSuccess) {
                          return Uni.createFrom()
                              .item(
                                  new ServiceActionResponse<>(
                                      ResourceType.FILE,
                                      ActionType.DELETE,
                                      List.of(
                                          ServiceError.builder()
                                              .errorCode(
                                                  ErrorCode.FILE_TAG_MAPPING_UNABLE_TO_DELETE)
                                              .errorMessage(
                                                  "Unable to delete File Tag Mapping '"
                                                      + file.getId()
                                                      + "'")
                                              .httpCode(500)
                                              .build())));
                        }

                        return artifactRepo
                            .getArtifactsByFileId(file.getId(), userId)
                            .chain(
                                artifacts -> {
                                  List<String> keysToDelete = new ArrayList<>();
                                  keysToDelete.add(file.getStorageKey());
                                  if (artifacts != null && !artifacts.isEmpty()) {
                                    artifacts.forEach(
                                        artifact -> keysToDelete.add(artifact.getStorageKey()));
                                  }

                                  return storageService
                                      .bulkDelete(keysToDelete)
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
                                                                  .errorMessage(
                                                                      "Error deleting file.")
                                                                  .httpCode(500)
                                                                  .build())));
                                            }

                                            return artifactRepo
                                                .deleteArtifactsByFileId(file.getId(), userId)
                                                .chain(
                                                    deleteSuccess -> {
                                                      if (!deleteSuccess) {
                                                        return Uni.createFrom()
                                                            .item(
                                                                new ServiceActionResponse<>(
                                                                    ResourceType.FILE,
                                                                    ActionType.DELETE,
                                                                    List.of(
                                                                        ServiceError.builder()
                                                                            .errorCode(
                                                                                ErrorCode
                                                                                    .UNABLE_TO_DELETE_ARTIFACTS)
                                                                            .errorMessage(
                                                                                "Error deleting artifact records from DB.")
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
                                                                              .errorCode(
                                                                                  ErrorCode
                                                                                      .UNABLE_TO_DELETE_FILE)
                                                                              .errorMessage(
                                                                                  "Unable to delete file")
                                                                              .httpCode(400)
                                                                              .build()));
                                                                }

                                                                return new ServiceActionResponse<>(
                                                                    ResourceType.FILE,
                                                                    ActionType.DELETE,
                                                                    file);
                                                              });
                                                    });
                                          });
                                });
                      });
            });
  }

  public Uni<ServiceResponse<DownloadData>> getFileForDownload(
      final long fileId, final UserToken userInfo) {
    return internalGetFileForDownload(fileId, userInfo, false);
  }

  public Uni<ServiceResponse<DownloadData>> getFileThumbnailForDownload(
      final long fileId, final UserToken userInfo) {
    return internalGetFileForDownload(fileId, userInfo, true);
  }

  private Uni<ServiceResponse<DownloadData>> internalGetFileForDownload(
      final long fileId, final UserToken userInfo, final boolean getThumbnail) {
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
                        String storageKey = fileMetadata.getStorageKey();
                        if (getThumbnail) {
                          storageKey += ".thumb.jpg";
                        }

                        return storageService
                            .getFileData(storageKey)
                            .map(
                                fileDataStream -> {
                                  if (fileDataStream == null) {
                                    return new ServiceResponse<>(
                                        new ServiceActionResponse<>(
                                            ResourceType.FILE,
                                            ActionType.DOWNLOAD,
                                            List.of(
                                                ServiceError.builder()
                                                    .errorCode(ErrorCode.IO_FILE_DOES_NOT_EXIST)
                                                    .errorMessage(
                                                        "The file you are requesting doesn't exist.")
                                                    .httpCode(404)
                                                    .build())));
                                  }

                                  String filename = fileMetadata.getOriginalFilename();
                                  if (getThumbnail) {
                                    int lio = filename.lastIndexOf('.');
                                    if (lio != -1) {
                                      filename = filename.substring(0, lio);
                                    }
                                    filename += ".thumb.jpg";
                                  }

                                  return new ServiceResponse<>(
                                      new ServiceActionResponse<>(
                                          ResourceType.FILE,
                                          ActionType.DOWNLOAD,
                                          new DownloadData(filename, fileDataStream)));
                                });
                      } catch (Exception e) {
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

  <T> Uni<ServiceResponse<T>> checkFileExists(
      final long fileId,
      final long userId,
      final ResourceType resourceType,
      final ActionType actionType,
      final Function<File, Uni<ServiceResponse<T>>> fileExistAction) {
    return storedFilesRepo
        .getStoredFile(fileId, userId)
        .chain(
            file -> {
              if (file == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                resourceType,
                                actionType,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.FILE_DOES_NOT_EXIST)
                                        .errorMessage(
                                            "Operation could not complete because file '"
                                                + fileId
                                                + "' does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return fileExistAction.apply(file);
            });
  }

  public Uni<ServiceResponse<Boolean>> assignTag(
      final UserToken userToken, final long fileId, final long tagId) {
    return userService.checkUserExist(
        userToken,
        ResourceType.TAG,
        ActionType.ASSIGN,
        user ->
            checkFileExists(
                fileId,
                user.getId(),
                ResourceType.TAG,
                ActionType.ASSIGN,
                file ->
                    tagService.checkIfTagExists(
                        user.getId(),
                        tagId,
                        ResourceType.TAG,
                        ActionType.ASSIGN,
                        tag ->
                            fileTagsRepo
                                .assignFileMapping(fileId, tagId)
                                .map(
                                    assignFileMappingSuccess ->
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                ResourceType.TAG,
                                                ActionType.ASSIGN,
                                                assignFileMappingSuccess))))));
  }

  public Uni<ServiceResponse<Boolean>> unassignTag(
      final UserToken userToken, final long fileId, final long tagId) {
    return userService.checkUserExist(
        userToken,
        ResourceType.TAG,
        ActionType.UNASSIGN,
        user ->
            checkFileExists(
                fileId,
                user.getId(),
                ResourceType.TAG,
                ActionType.UNASSIGN,
                file ->
                    tagService.checkIfTagExists(
                        user.getId(),
                        tagId,
                        ResourceType.TAG,
                        ActionType.UNASSIGN,
                        tag ->
                            fileTagsRepo
                                .unassignFileMapping(fileId, tagId)
                                .map(
                                    unassignFileTagSuccess ->
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                ResourceType.TAG,
                                                ActionType.UNASSIGN,
                                                unassignFileTagSuccess))))));
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> searchFiles(
      final UserToken userToken,
      final String searchTerm,
      final PaginationParameters paginationParameters) {

    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.SEARCH,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_SEARCH_TEXT)
                              .errorMessage("Search text cannot be empty")
                              .httpCode(400)
                              .build()))));
    }

    if (paginationParameters.getAfter() != null && paginationParameters.getAfter() < 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.SEARCH,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_AFTER_VALUE)
                              .errorMessage("After value must be greater than 0.")
                              .httpCode(400)
                              .build()))));
    }

    if (paginationParameters.getLimit() != null && paginationParameters.getLimit() <= 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FILE,
                      ActionType.SEARCH,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.INVALID_RESPONSE_LIMIT)
                              .errorMessage(
                                  "A return limit of '"
                                      + paginationParameters.getLimit()
                                      + "' is invalid. Must be greater than 0")
                              .httpCode(400)
                              .build()))));
    }

    return userService.checkUserExist(
        userToken,
        ResourceType.FILE,
        ActionType.SEARCH,
        user ->
            storedFilesRepo
                .searchFiles(user.getId(), searchTerm.trim(), paginationParameters)
                .map(
                    paginatedFileData ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FILE,
                                ActionType.SEARCH,
                                paginationMapper.toPaginatedResponse(paginatedFileData)))));
  }
}
