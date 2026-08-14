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
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.FileUploadContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.config.FilearchConfig;
import com.xnelo.filearch.restapi.data.FileTagsRepo;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
import com.xnelo.filearch.restapi.data.SequenceRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import com.xnelo.filearch.restapi.service.folder.FolderService;
import com.xnelo.filearch.restapi.service.tag.TagService;
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
  @Inject FileService fileService;
  @Inject ArtifactRepo artifactRepo;
  @Inject FolderService folderService;
  @Inject FileTagsRepo fileTagsRepo;
  @Inject FilearchConfig config;
  @Inject TagService tagService;
  @Inject GroupItemsRepo groupItemsRepo;
  @Inject GroupItemService groupItemService;
  @Inject GroupService groupService;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);
  final MessagingMapper messagingMapper = Mappers.getMapper(MessagingMapper.class);

  @Channel("file-proc-requests")
  Emitter<String> fileProcRequestEmitter;

  public static final String GET_THUMBNAIL_KEY = "GET_THUMBNAIL__BOOLEAN";
  public static final String FILE_ID_KEY = "FILE_ID__LONG";
  public static final String FILE_METADATA_KEY = "FILE_METADATA__FILE";

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFiles(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    ServiceResponse<PaginatedResponse<File>> res =
        Utils.validatePaginationParameters(requestContext, paginationParameters);
    if (res != null) {
      return Uni.createFrom().item(res);
    }

    return userService.checkUserExist(
        requestContext,
        context1 ->
            storedFilesRepo
                .getAll(context1.getUser().getId(), paginationParameters)
                .map(
                    paginatedFiles ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                paginationMapper.toPaginatedResponse(paginatedFiles)))));
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
      final ServiceRequestContext requestContext, final FileUploadContract toUpload) {
    return userService.checkUserExist(
        requestContext,
        context -> {
          if (toUpload.folderId != null) {
            return folderService.checkFolderExist(
                context,
                toUpload.folderId,
                context2 -> uploadAllFiles(context2, toUpload.folderId, toUpload.files));
          } else {
            return uploadAllFiles(context, context.getUser().getRootFolderId(), toUpload.files);
          }
        });
  }

  Uni<ServiceResponse<File>> uploadAllFiles(
      final ServiceRequestContext requestContext,
      final long folderId,
      final List<FileUpload> files) {
    ArrayList<Uni<ServiceActionResponse<File>>> uploadResults = new ArrayList<>();

    for (final FileUpload fileToUpload : files) {
      uploadResults.add(uploadIndividualFile(requestContext, folderId, fileToUpload));
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
      final ServiceRequestContext requestContext,
      final Long folderId,
      final FileUpload fileToUpload) {
    return createStorageKey(requestContext.getUser())
        .chain(
            uploadKey ->
                storageService
                    .save(fileToUpload, uploadKey)
                    .chain(
                        errorCode -> {
                          if (errorCode != ErrorCode.OK) {
                            return Uni.createFrom()
                                .item(
                                    Utils.createServiceActionErrorResponse(
                                        requestContext,
                                        errorCode,
                                        "Unable to store file '" + fileToUpload.fileName() + "'",
                                        400));
                          } else {
                            return storedFilesRepo
                                .createStoredFile(
                                    requestContext.getUser().getId(),
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

  public Uni<ServiceResponse<File>> getFileMetadata(
      final ServiceRequestContext requestContext, final long fileId) {
    return userService.checkUserExist(
        requestContext,
        context ->
            storedFilesRepo
                .getStoredFile(fileId, context.getUser().getId())
                .chain(
                    file -> {
                      if (file == null) {
                        return Uni.createFrom()
                            .item(
                                new ServiceResponse<>(
                                    new ServiceActionResponse<>(
                                        context.getResourceType(),
                                        context.getActionType(),
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
                    }));
  }

  @SuppressWarnings("unchecked")
  public Uni<ServiceResponse<File>> deleteFile(
      final ServiceRequestContext requestContext, final long fileId) {
    return userService.checkUserExist(
        requestContext, context -> deleteIndividualFile(context, fileId).map(ServiceResponse::new));
  }

  private Uni<ServiceActionResponse<File>> deleteIndividualFile(
      final ServiceRequestContext requestContext, final long fileId) {
    return fileService.checkFileExistsActionResponse(
        requestContext,
        fileId,
        context ->
            fileTagsRepo
                .deleteAllFileMappings(fileId)
                .chain(
                    deleteMappingsSuccess -> {
                      if (!deleteMappingsSuccess) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceActionErrorResponse(
                                    context,
                                    ErrorCode.FILE_TAG_MAPPING_UNABLE_TO_DELETE,
                                    "Unable to delete Tile Tag Mapping '" + fileId + "'.",
                                    500));
                      }

                      return groupItemsRepo
                          .removeItemFromAllGroups(fileId, GroupItemType.FILE)
                          .chain(
                              deleteGroupItemsSuccess -> {
                                if (!deleteGroupItemsSuccess) {
                                  return Uni.createFrom()
                                      .item(
                                          Utils.createServiceActionErrorResponse(
                                              context,
                                              ErrorCode.UNABLE_TO_REMOVE_ITEM_FROM_GROUP,
                                              "Error while deleting item from all Groups",
                                              500));
                                }

                                return artifactRepo
                                    .getArtifactsByFileId(fileId, context.getUser().getId())
                                    .chain(
                                        artifacts -> {
                                          File fileData =
                                              context.getDataAs(FILE_METADATA_KEY, File.class);

                                          List<String> keysToDelete = new ArrayList<>();
                                          keysToDelete.add(fileData.getStorageKey());
                                          if (artifacts != null && !artifacts.isEmpty()) {
                                            artifacts.forEach(
                                                artifact ->
                                                    keysToDelete.add(artifact.getStorageKey()));
                                          }

                                          return storageService
                                              .bulkDelete(keysToDelete)
                                              .chain(
                                                  storageDeleteResult -> {
                                                    if (storageDeleteResult != ErrorCode.OK) {
                                                      return Uni.createFrom()
                                                          .item(
                                                              Utils
                                                                  .createServiceActionErrorResponse(
                                                                      context,
                                                                      storageDeleteResult,
                                                                      "Error deleting file.",
                                                                      500));
                                                    }

                                                    return artifactRepo
                                                        .deleteArtifactsByFileId(
                                                            fileId, context.getUser().getId())
                                                        .chain(
                                                            deleteSuccess -> {
                                                              if (!deleteSuccess) {
                                                                return Uni.createFrom()
                                                                    .item(
                                                                        Utils
                                                                            .createServiceActionErrorResponse(
                                                                                context,
                                                                                ErrorCode
                                                                                    .UNABLE_TO_DELETE_ARTIFACTS,
                                                                                "Error deleting artifact records from DB.",
                                                                                500));
                                                              }

                                                              return storedFilesRepo
                                                                  .deleteStoredFile(
                                                                      fileId,
                                                                      context.getUser().getId())
                                                                  .map(
                                                                      deleteSuccessful -> {
                                                                        if (!deleteSuccessful) {
                                                                          return Utils
                                                                              .createServiceActionErrorResponse(
                                                                                  context,
                                                                                  ErrorCode
                                                                                      .UNABLE_TO_DELETE_FILE,
                                                                                  "Unable to delete file",
                                                                                  400);
                                                                        }

                                                                        return new ServiceActionResponse<>(
                                                                            ResourceType.FILE,
                                                                            ActionType.DELETE,
                                                                            fileData);
                                                                      });
                                                            });
                                                  });
                                        });
                              });
                    }));
  }

  public Uni<ServiceResponse<DownloadData>> getFileForDownload(
      final ServiceRequestContext requestContext) {
    return userService.checkUserExist(
        requestContext,
        context -> {
          final long fileId = requestContext.getLongData(FILE_ID_KEY);
          if (requestContext.getGroupId() == null) {
            // get a file ensuring that the requesting user owns it
            return storedFilesRepo
                .getStoredFile(fileId, requestContext.getUser().getId())
                .chain(fileMetadata -> internalGetFileFromMetadata(context, fileMetadata));
          } else {
            // get a file in the context of being a member of the group
            return groupService.checkUserActiveMemberInGroup(
                requestContext,
                context2 ->
                    groupItemService.checkItemInGroup(
                        context2,
                        fileId,
                        GroupItemType.FILE,
                        context3 ->
                            storedFilesRepo
                                .getStoredFile(fileId)
                                .chain(
                                    fileMetadata ->
                                        internalGetFileFromMetadata(context3, fileMetadata))));
          }
        });
  }

  private Uni<ServiceResponse<DownloadData>> internalGetFileFromMetadata(
      final ServiceRequestContext context, final File fileMetadata) {
    if (fileMetadata == null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      context.getResourceType(),
                      context.getActionType(),
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.FILE_DOES_NOT_EXIST)
                              .errorMessage("File does not exist")
                              .httpCode(404)
                              .build()))));
    }

    try {
      String storageKey = fileMetadata.getStorageKey();
      final boolean getThumbnail = context.getBooleanData(GET_THUMBNAIL_KEY, false);
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
                          context.getResourceType(),
                          context.getActionType(),
                          List.of(
                              ServiceError.builder()
                                  .errorCode(ErrorCode.IO_FILE_DOES_NOT_EXIST)
                                  .errorMessage("The file you are requesting doesn't exist.")
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
                        context.getResourceType(),
                        context.getActionType(),
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
                      context.getResourceType(),
                      context.getActionType(),
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.UNABLE_TO_OPEN_INPUT_STREAM)
                              .errorMessage("Unable to open Input Stream for file.")
                              .httpCode(500)
                              .build()))));
    }
  }

  public Uni<List<File>> getFilesInFoldersInternal(final List<Long> folderIds, final long userId) {
    return storedFilesRepo.getFilesInFolders(folderIds, userId);
  }

  public Uni<ServiceResponse<File>> bulkDeleteFiles(
      final ServiceRequestContext requestContext,
      final List<Long> filesIdsToDelete,
      final boolean userCheckNeeded) {
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

    if (userCheckNeeded) {
      return userService.checkUserExist(
          requestContext, context -> bulkDeleteFilesLoop(context, filesIdsToDelete));
    } else {
      return bulkDeleteFilesLoop(requestContext, filesIdsToDelete);
    }
  }

  private Uni<ServiceResponse<File>> bulkDeleteFilesLoop(
      final ServiceRequestContext requestContext, List<Long> filesIdsToDelete) {
    ArrayList<Uni<ServiceActionResponse<File>>> fileDeleteUnis =
        new ArrayList<>(filesIdsToDelete.size());
    filesIdsToDelete.forEach(
        fileIdToDelete -> fileDeleteUnis.add(deleteIndividualFile(requestContext, fileIdToDelete)));
    return Uni.combine().all().unis(fileDeleteUnis).with(FileService::combineFileActionUnis);
  }

  public <T> Uni<ServiceActionResponse<T>> checkFileExistsActionResponse(
      final ServiceRequestContext requestContext,
      final long fileId,
      final Function<ServiceRequestContext, Uni<ServiceActionResponse<T>>> fileExistAction) {
    return internalCheckFileExists(
        requestContext, fileId, fileExistAction, Utils::createServiceActionErrorResponse);
  }

  public <T> Uni<ServiceResponse<T>> checkFileExists(
      final ServiceRequestContext requestContext,
      final long fileId,
      final Function<ServiceRequestContext, Uni<ServiceResponse<T>>> fileExistAction) {
    return internalCheckFileExists(
        requestContext, fileId, fileExistAction, Utils::createServiceErrorResponse);
  }

  @FunctionalInterface
  private interface CreateErrorResponseFunction<Z> {
    Z apply(
        ServiceRequestContext requestContext,
        ErrorCode errorCode,
        String errorMessage,
        Integer httpCode);
  }

  private <Z> Uni<Z> internalCheckFileExists(
      final ServiceRequestContext requestContext,
      final long fileId,
      final Function<ServiceRequestContext, Uni<Z>> fileExistAction,
      final CreateErrorResponseFunction<Z> createErrorResponseFunc) {
    if (requestContext.getUser() == null) {
      Log.error(
          "User was not provided in the requestContext. Please retrieve and set the user in the request context before using this method.");
      return Uni.createFrom()
          .item(
              createErrorResponseFunc.apply(
                  requestContext,
                  ErrorCode.USER_NOT_PROVIDED_IN_REQUEST_OBJECT,
                  "User was null in the request object. Please contact support.",
                  500));
    }

    return storedFilesRepo
        .getStoredFile(fileId, requestContext.getUser().getId())
        .chain(
            file -> {
              if (file == null) {
                return Uni.createFrom()
                    .item(
                        createErrorResponseFunc.apply(
                            requestContext,
                            ErrorCode.FILE_DOES_NOT_EXIST,
                            "Operation could not complete because file '"
                                + fileId
                                + "' does not exist.",
                            404));
              }

              requestContext.setData(FILE_METADATA_KEY, file);

              return fileExistAction.apply(requestContext);
            });
  }

  @Deprecated
  public <T> Uni<ServiceResponse<T>> checkFileExists(
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
      final ServiceRequestContext requestContext, final long fileId, final long tagId) {
    return userService.checkUserExist(
        requestContext,
        context ->
            checkFileExists(
                requestContext,
                fileId,
                context2 ->
                    tagService.checkIfTagExists(
                        context2,
                        tagId,
                        context3 ->
                            fileTagsRepo
                                .assignFileMapping(fileId, tagId)
                                .map(
                                    res ->
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                context3.getResourceType(),
                                                context3.getActionType(),
                                                res))))));
  }

  public Uni<ServiceResponse<Boolean>> unassignTag(
      final ServiceRequestContext requestContext, final long fileId, final long tagId) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            checkFileExists(
                context2,
                fileId,
                context3 ->
                    tagService.checkIfTagExists(
                        context3,
                        tagId,
                        context4 ->
                            fileTagsRepo
                                .unassignFileMapping(fileId, tagId)
                                .map(
                                    res ->
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                context4.getResourceType(),
                                                context4.getActionType(),
                                                res))))));
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> searchFiles(
      final ServiceRequestContext requestContext, final SearchParameters searchParameters) {

    if (searchParameters.getSearchTerm() == null
        || searchParameters.getSearchTerm().trim().isEmpty()) {
      return Uni.createFrom()
          .item(
              Utils.createServiceErrorResponse(
                  requestContext,
                  ErrorCode.INVALID_SEARCH_TEXT,
                  "Search text cannot be empty",
                  400));
    }
    ServiceResponse<PaginatedResponse<File>> validationResponse =
        Utils.validatePaginationParameters(requestContext, searchParameters);
    if (validationResponse != null) {
      return Uni.createFrom().item(validationResponse);
    }

    return userService.checkUserExist(
        requestContext,
        context2 ->
            storedFilesRepo
                .searchFiles(requestContext.getUser().getId(), searchParameters)
                .map(
                    paginatedFileData ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(),
                                context2.getActionType(),
                                paginationMapper.toPaginatedResponse(paginatedFileData)))));
  }
}
