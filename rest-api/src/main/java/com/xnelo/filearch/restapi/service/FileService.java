package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.data.ArtifactRepo;
import com.xnelo.filearch.common.exception.RepoException;
import com.xnelo.filearch.common.exception.ServiceResponseException;
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
  @Inject GroupPermissionsService groupPermissionsService;
  @Inject SharedTagsService shareDtagsService;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);
  final MessagingMapper messagingMapper = Mappers.getMapper(MessagingMapper.class);

  @Channel("file-proc-requests")
  Emitter<String> fileProcRequestEmitter;

  public static final String GET_THUMBNAIL_KEY = "GET_THUMBNAIL__BOOLEAN";
  public static final String FILE_ID_KEY = "FILE_ID__LONG";
  public static final String FILE_METADATA_KEY = "FILE_METADATA__FILE";

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFiles(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService
        .checkUserExist(requestContext)
        .chain(
            context ->
                storedFilesRepo
                    .getAll(context.getUser().getId(), paginationParameters)
                    .map(
                        paginatedFiles ->
                            new ServiceResponse<>(
                                new ServiceActionResponse<>(
                                    context.getResourceType(),
                                    context.getActionType(),
                                    paginationMapper.toPaginatedResponse(paginatedFiles)))));
  }

  public Uni<ServiceResponse<List<Long>>> getAllFileIdsInFolder(
      final ServiceRequestContext requestContext, final Long folderId) {
    return userService
        .checkUserExist(requestContext)
        .chain(context -> storedFilesRepo.getFileIdsInFolder(context.getUser().getId(), folderId))
        .map(
            fileIds ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        fileIds)));
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFilesInFolder(
      final ServiceRequestContext requestContext,
      final Long folderId,
      final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService
        .checkUserExist(requestContext)
        .chain(
            context ->
                storedFilesRepo.getFilesInFolder(
                    folderId, context.getUser().getId(), paginationParameters))
        .map(
            paginatedFiles ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        ResourceType.FILE,
                        ActionType.GET,
                        paginationMapper.toPaginatedResponse(paginatedFiles))));
  }

  public Uni<ServiceResponse<File>> uploadFiles(
      final ServiceRequestContext requestContext, final FileUploadContract toUpload) {
    return userService
        .checkUserExist(requestContext)
        .chain(
            context -> {
              if (toUpload.folderId != null) {
                return folderService
                    .checkFolderExist(context, toUpload.folderId)
                    .chain(context2 -> uploadAllFiles(context, toUpload.folderId, toUpload.files));
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
    Utils.checkUserInRequest(requestContext);

    return createStorageKey(requestContext.getUser())
        .chain(
            uploadKey ->
                storageService
                    .save(fileToUpload, uploadKey)
                    .chain(
                        errorCode -> {
                          if (errorCode != ErrorCode.OK) {
                            throw new ServiceResponseException(
                                requestContext,
                                errorCode,
                                "Unable to store file '" + fileToUpload.fileName() + "'",
                                400);
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
                                            requestContext.getResourceType(),
                                            requestContext.getActionType(),
                                            dbFile));
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
    return userService
        .checkUserExist(requestContext)
        .chain(context -> storedFilesRepo.getStoredFile(fileId, context.getUser().getId()))
        .map(
            fileMetadata -> {
              if (fileMetadata == null) {
                throw new ServiceResponseException(
                    requestContext, ErrorCode.FILE_DOES_NOT_EXIST, "File was not found.", 404);
              }

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
                      fileMetadata));
            });
  }

  @SuppressWarnings("unchecked")
  public Uni<ServiceResponse<File>> deleteFile(
      final ServiceRequestContext requestContext, final long fileId) {
    return userService
        .checkUserExist(requestContext)
        .chain(context -> deleteIndividualFile(context, fileId).map(ServiceResponse::new));
  }

  private Uni<List<String>> gatherAllRelatedFiles(
      final ServiceRequestContext requestContext, final long fileId) {
    return artifactRepo
        .getArtifactsByFileId(fileId, requestContext.getUser().getId())
        .map(
            artifacts -> {
              File fileData = requestContext.getDataAs(FILE_METADATA_KEY, File.class);
              if (fileData == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.DATA_OBJECT_NOT_PROVIDED_IN_REQUEST_OBJECT,
                    "File was not found in request object. Contact support.",
                    500);
              }

              List<String> keysToDelete = new ArrayList<>();
              keysToDelete.add(fileData.getStorageKey());
              if (artifacts != null && !artifacts.isEmpty()) {
                artifacts.forEach(artifact -> keysToDelete.add(artifact.getStorageKey()));
              }
              return keysToDelete;
            });
  }

  private Uni<Boolean> deleteAllFilesFromStorage(
      final ServiceRequestContext context, final List<String> keysToDelete) {
    return storageService
        .bulkDelete(keysToDelete)
        .map(
            storageDeleteResult -> {
              if (storageDeleteResult != ErrorCode.OK) {
                throw new ServiceResponseException(
                    context, storageDeleteResult, "Error deleting file.", 500);
              }

              return true;
            });
  }

  private Uni<ServiceActionResponse<File>> deleteIndividualFile(
      final ServiceRequestContext requestContext, final long fileId) {
    return fileService
        .checkFileExist(requestContext, fileId)
        .chain(_ignored -> fileTagsRepo.deleteAllFileMappings(fileId))
        .chain(_ignored -> groupItemsRepo.removeItemFromAllGroups(fileId, GroupItemType.FILE))
        .chain(_ignored -> gatherAllRelatedFiles(requestContext, fileId))
        .chain(keysToDelete -> deleteAllFilesFromStorage(requestContext, keysToDelete))
        .chain(
            _ignored ->
                artifactRepo.deleteArtifactsByFileId(fileId, requestContext.getUser().getId()))
        .chain(
            _ignored -> storedFilesRepo.deleteStoredFile(fileId, requestContext.getUser().getId()))
        .map(
            success -> {
              if (!success) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.UNABLE_TO_DELETE_FILE,
                    "File was unable to be deleted",
                    500);
              }

              File fileData = requestContext.getDataAs(FILE_METADATA_KEY, File.class);
              if (fileData == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.DATA_OBJECT_NOT_PROVIDED_IN_REQUEST_OBJECT,
                    "File was not found in request object. Contact support.",
                    500);
              }

              return new ServiceActionResponse<>(
                  requestContext.getResourceType(), requestContext.getActionType(), fileData);
            })
        .onFailure(RepoException.class)
        .transform(
            ex ->
                new ServiceResponseException(
                    requestContext, ex.getErrorCode(), ex.getMessage(), 500));
  }

  public Uni<ServiceResponse<DownloadData>> getFileForDownload(
      final ServiceRequestContext requestContext) {
    return userService
        .checkUserExist(requestContext)
        .chain(
            context -> {
              final long fileId = context.getLongData(FILE_ID_KEY);
              if (context.getGroupId() == null) {
                // get a file ensuring that the requesting user owns it
                return storedFilesRepo
                    .getStoredFile(fileId, context.getUser().getId())
                    .chain(fileMetadata -> internalGetFileFromMetadata(context, fileMetadata));
              } else {
                // get a file in the context of being a member of the group
                return groupService
                    .checkUserActiveMember(context)
                    .chain(
                        context2 ->
                            groupItemService.checkItemInGroup(context, fileId, GroupItemType.FILE))
                    .chain(context2 -> storedFilesRepo.getStoredFile(fileId))
                    .chain(fileMetadata -> internalGetFileFromMetadata(context, fileMetadata));
              }
            });
  }

  private Uni<ServiceResponse<DownloadData>> internalGetFileFromMetadata(
      final ServiceRequestContext context, final File fileMetadata) {
    if (fileMetadata == null) {
      throw new ServiceResponseException(
          context, ErrorCode.FILE_DOES_NOT_EXIST, "File does not exist", 505);
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
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.TOO_MANY_BULK_OPERATIONS,
          "Maximum number of bulk operations for bulk delete is "
              + config.bulkActions().maxDelete()
              + ". "
              + filesIdsToDelete.size()
              + " were passed in.",
          400);
    }

    if (userCheckNeeded) {
      return userService
          .checkUserExist(requestContext)
          .chain(context -> bulkDeleteFilesLoop(context, filesIdsToDelete));
    } else {
      return bulkDeleteFilesLoop(requestContext, filesIdsToDelete);
    }
  }

  private Uni<ServiceResponse<File>> bulkDeleteFilesLoop(
      final ServiceRequestContext requestContext, List<Long> filesIdsToDelete) {
    ArrayList<Uni<ServiceActionResponse<File>>> fileDeleteUnis =
        new ArrayList<>(filesIdsToDelete.size());
    filesIdsToDelete.forEach(
        fileIdToDelete -> {
          Uni<ServiceActionResponse<File>> deleteIndividualFileUni =
              deleteIndividualFile(requestContext, fileIdToDelete)
                  .onFailure(ServiceResponseException.class)
                  .recoverWithItem(
                      ex ->
                          Utils.createServiceActionErrorResponse(
                              requestContext,
                              ex.getErrorCode(),
                              ex.getMessage(),
                              ex.getHttpStatus()));
          fileDeleteUnis.add(deleteIndividualFileUni);
        });
    return Uni.combine().all().unis(fileDeleteUnis).with(FileService::combineFileActionUnis);
  }

  public Uni<ServiceRequestContext> checkFileExist(
      ServiceRequestContext requestContext, Long fileId) {
    Utils.checkUserInRequest(requestContext);

    return storedFilesRepo
        .getStoredFile(fileId, requestContext.getUser().getId())
        .map(
            file -> {
              if (file == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.FILE_DOES_NOT_EXIST,
                    "File (" + fileId + ") does not exist.",
                    404);
              }

              requestContext.setData(FILE_METADATA_KEY, file);

              return requestContext;
            });
  }

  @Deprecated
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

  public Uni<ServiceResponse<Boolean>> assignTag(
      ServiceRequestContext requestContext, final long fileId, final long tagId) {

    Uni<Boolean> canUserAddTag;
    Long groupIdFinal = requestContext.getGroupId();

    if (requestContext.getGroupId() == null) {
      canUserAddTag =
          userService
              .checkUserExist(requestContext)
              .chain(context -> checkFileExist(context, fileId))
              .chain(context -> tagService.checkIfTagExists(context, tagId))
              .map(context -> true);
    } else {
      canUserAddTag =
          userService
              .checkUserExist(requestContext)
              .chain(context -> groupService.checkUserActiveMember(requestContext))
              .chain(
                  context ->
                      groupPermissionsService.userHasPermissionV2(
                          requestContext, GroupPermissionType.TAG_ITEMS))
              .chain(
                  context -> groupItemService.checkItemInGroup(context, fileId, GroupItemType.FILE))
              .chain(context -> shareDtagsService.checkTagShareExists(requestContext, tagId))
              .map(context -> true);
    }

    return canUserAddTag
        .chain(res -> fileTagsRepo.assignFileMapping(fileId, tagId, groupIdFinal))
        .map(
            res ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(), requestContext.getActionType(), res)));
  }

  public Uni<ServiceResponse<Boolean>> unassignTag(
      final ServiceRequestContext requestContext, final long fileId, final long tagId) {
    Uni<Boolean> canUserUnassignTag;
    Long groupIdFinal = requestContext.getGroupId();

    if (requestContext.getGroupId() == null) {
      canUserUnassignTag =
          userService
              .checkUserExist(requestContext)
              .chain(context -> checkFileExist(context, fileId))
              .chain(context -> tagService.checkIfTagExists(requestContext, tagId))
              .map(context -> true);
    } else {
      canUserUnassignTag =
          userService
              .checkUserExist(requestContext)
              .chain(context -> groupService.checkUserActiveMember(context))
              .chain(
                  context ->
                      groupPermissionsService.userHasPermissionV2(
                          context, GroupPermissionType.REMOVE_TAGS))
              .map(context -> true);
    }

    return canUserUnassignTag.chain(
        ignore ->
            fileTagsRepo
                .unassignFileMapping(fileId, tagId, groupIdFinal)
                .map(
                    res ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                res))));
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> searchFiles(
      final ServiceRequestContext requestContext, final SearchParameters searchParameters) {

    if (searchParameters.getSearchTerm() == null
        || searchParameters.getSearchTerm().trim().isEmpty()) {
      throw new ServiceResponseException(
          requestContext, ErrorCode.INVALID_SEARCH_TEXT, "Search text cannot be empty.", 400);
    }
    Utils.validatePaginationParameters(requestContext, searchParameters);

    return userService
        .checkUserExist(requestContext)
        .chain(
            context2 ->
                storedFilesRepo.searchFiles(requestContext.getUser().getId(), searchParameters))
        .map(
            paginatedFileData ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        paginationMapper.toPaginatedResponse(paginatedFileData))));
  }
}
