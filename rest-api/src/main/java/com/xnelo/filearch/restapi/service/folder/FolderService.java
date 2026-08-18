package com.xnelo.filearch.restapi.service.folder;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.utils.Lists;
import com.xnelo.filearch.restapi.api.contracts.FolderContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.config.FilearchConfig;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.service.FileService;
import com.xnelo.filearch.restapi.service.UserService;
import com.xnelo.filearch.restapi.service.Utils;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class FolderService {
  @Inject FolderRepo folderRepo;
  @Inject UserService userService;
  @Inject FileService fileService;
  @Inject FilearchConfig config;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  public Uni<Folder> createRootFolderForUser(final long userId) {
    return folderRepo.createRootFolder(userId);
  }

  public Uni<ServiceResponse<Folder>> deleteRootFolder(
      final ServiceRequestContext requestContext, final long folderId) {
    return deleteIfFolderExists(requestContext, folderId, false);
  }

  public Uni<ServiceResponse<PaginatedResponse<Folder>>> getAllFolders(
      final ServiceRequestContext requestContext, final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService.checkUserExist(
        requestContext,
        context2 ->
            folderRepo
                .getAll(context2.getUser().getId(), paginationParameters)
                .map(
                    paginatedFolders ->
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                context2.getResourceType(),
                                context2.getActionType(),
                                paginationMapper.toPaginatedResponse(paginatedFolders)))));
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFilesInFolder(
      final ServiceRequestContext requestContext,
      final Long folderId,
      final PaginationParameters paginationParameters) {

    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService.checkUserExist(
        requestContext,
        context2 ->
            checkFolderExist(
                context2,
                folderId,
                context3 ->
                    fileService.getAllFilesInFolder(context3, folderId, paginationParameters)));
  }

  public Uni<ServiceResponse<List<Long>>> getAllFileIdsInFolder(
      final ServiceRequestContext requestContext, final long folderId) {
    return fileService.getAllFileIdsInFolder(requestContext, folderId);
  }

  public <T> Uni<ServiceResponse<T>> checkFolderExist(
      final ServiceRequestContext context,
      final long folderId,
      final Function<ServiceRequestContext, Uni<ServiceResponse<T>>> folderExistAction) {
    return folderRepo
        .getFolderById(folderId, context.getUser().getId())
        .chain(
            folderInfo -> {
              if (folderInfo == null) {
                return Uni.createFrom()
                    .item(
                        Utils.createServiceErrorResponse(
                            context,
                            ErrorCode.FOLDER_DOES_NOT_EXIST,
                            "Folder (" + folderId + ") does not exist.",
                            404));
              }

              return folderExistAction.apply(context);
            });
  }

  public Uni<ServiceResponse<Folder>> createNewFolder(
      final ServiceRequestContext requestContext, final FolderContract newFolder) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            checkFolderExist(
                requestContext,
                newFolder.getParentId(),
                context3 ->
                    folderRepo
                        .nameExistInFolder(
                            newFolder.getFolderName(),
                            newFolder.getParentId(),
                            context3.getUser().getId())
                        .chain(
                            nameExists -> {
                              if (nameExists) {
                                return Uni.createFrom()
                                    .item(
                                        Utils.createServiceErrorResponse(
                                            requestContext,
                                            ErrorCode.FOLDER_WITH_NAME_ALREADY_EXISTS,
                                            "A folder with the name '"
                                                + newFolder.getFolderName()
                                                + "' already exists.",
                                            400));
                              }

                              return folderRepo
                                  .createFolder(
                                      context3.getUser().getId(),
                                      newFolder.getParentId(),
                                      newFolder.getFolderName())
                                  .map(
                                      newlyCreatedFolder ->
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  context3.getResourceType(),
                                                  context3.getActionType(),
                                                  newlyCreatedFolder)));
                            })));
  }

  public Uni<ServiceResponse<Folder>> getFolderById(
      final ServiceRequestContext requestContext, final long folderId) {
    return userService.checkUserExist(
        requestContext,
        context2 ->
            folderRepo
                .getFolderById(folderId, context2.getUser().getId())
                .map(
                    folder -> {
                      if (folder == null) {
                        return new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                List.of(
                                    ServiceError.builder()
                                        .httpCode(404)
                                        .errorCode(ErrorCode.FOLDER_DOES_NOT_EXIST)
                                        .errorMessage("Folder does not exist.")
                                        .build())));
                      }

                      return new ServiceResponse<>(
                          new ServiceActionResponse<>(
                              requestContext.getResourceType(),
                              requestContext.getActionType(),
                              folder));
                    }));
  }

  public Uni<ServiceResponse<Folder>> updateFolder(
      final ServiceRequestContext requestContext,
      final long folderId,
      final FolderContract folderData) {
    if (folderData.getId() != null) {
      return Uni.createFrom()
          .item(
              Utils.createServiceErrorResponse(
                  requestContext,
                  ErrorCode.FOLDER_ID_CANNOT_BE_UPDATED,
                  "Folder id cannot be updated.",
                  400));
    } else if (folderData.getOwnerId() != null) {
      return Uni.createFrom()
          .item(
              Utils.createServiceErrorResponse(
                  requestContext,
                  ErrorCode.FOLDER_OWNER_CANNOT_BE_UPDATED,
                  "Folder Owner cannot be updated.",
                  400));
    }

    return userService.checkUserExist(
        requestContext,
        context2 ->
            folderRepo
                .getFolderById(folderId, context2.getUser().getId())
                .chain(
                    folderMetadata -> {
                      if (folderMetadata == null) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    context2,
                                    ErrorCode.FOLDER_DOES_NOT_EXIST,
                                    "Folder does not exist.",
                                    404));
                      }

                      // Check if this is the root folder
                      if (folderMetadata.isRootFolder()) {
                        return Uni.createFrom()
                            .item(
                                Utils.createServiceErrorResponse(
                                    context2,
                                    ErrorCode.ROOT_FOLDER_CANNOT_BE_UPDATED,
                                    "Root Folder cannot be updated.",
                                    400));
                      }

                      ArrayList<Uni<Map<String, Object>>> checkUnis = new ArrayList<>();
                      long destFolderId = folderId;
                      if (folderData.getParentId() != null) {
                        checkUnis.add(
                            updateFolderLocation(
                                context2,
                                folderMetadata.getFolderName(),
                                folderData.getParentId()));
                        destFolderId = folderData.getParentId();
                      }

                      if (folderData.getFolderName() != null) {
                        checkUnis.add(
                            updateFolderName(context2, destFolderId, folderData.getFolderName()));
                      }

                      Uni<Map<String, Object>> folderUpdates =
                          Uni.combine().all().unis(checkUnis).with(this::combineUpdateChecks);

                      return folderUpdates.chain(
                          folderUpdateMap -> {
                            if (folderUpdateMap.isEmpty()) {
                              return Uni.createFrom()
                                  .item(
                                      Utils.createServiceErrorResponse(
                                          context2,
                                          ErrorCode.NO_FIELDS_TO_UPDATE,
                                          "No Fields to update.",
                                          400));
                            }

                            return folderRepo
                                .updateFolder(folderId, context2.getUser().getId(), folderUpdateMap)
                                .map(
                                    updatedFolder ->
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                context2.getResourceType(),
                                                context2.getActionType(),
                                                updatedFolder)));
                          });
                    }));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> combineUpdateChecks(List<?> toCombine) {
    Map<String, Object> folderUpdates = new HashMap<>();
    for (Object folderUpdate : toCombine) {
      if (folderUpdate instanceof Map) {
        Map<String, Object> castFolderUpdate = (Map<String, Object>) folderUpdate;
        folderUpdates.putAll(castFolderUpdate);
      }
    }
    return folderUpdates;
  }

  private Uni<ServiceResponse<Folder>> updateErrorAndPassThrough(ServiceResponse<?> response) {
    ArrayList<ServiceActionResponse<Folder>> actionResponses = new ArrayList<>();
    for (ServiceActionResponse<?> actionResponse : response.getActionResponses()) {
      actionResponses.add(
          new ServiceActionResponse<>(
              ResourceType.FOLDER, actionResponse.getActionType(), actionResponse.getErrors()));
    }
    return Uni.createFrom().item(new ServiceResponse<>(actionResponses));
  }

  private Uni<Map<String, Object>> updateFolderName(
      final ServiceRequestContext requestContext, final long folderId, final String newFolderName) {
    // Preconditions:
    // * The folder to move exists
    // * The user exists

    return folderRepo
        .nameExistInFolder(newFolderName, folderId, requestContext.getUser().getId())
        .map(
            folderNameExists -> {
              if (folderNameExists) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.FOLDER_WITH_NAME_ALREADY_EXISTS,
                    "Folder already has another folder named " + newFolderName,
                    400);
              }
              return Map.of(FolderRepo.FOLDER_NAME_COLUMN_NAME, newFolderName);
            });
  }

  private Uni<Map<String, Object>> updateFolderLocation(
      final ServiceRequestContext requestContext, final String folderName, final long newParentId) {
    // Preconditions:
    // * The folder to move exists
    // * The user exists

    return getFolderById(requestContext, newParentId)
        .chain(
            folderResponse -> {
              if (folderResponse.hasError()) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.FOLDER_DOES_NOT_EXIST,
                    "New Parent folder does not exist.",
                    404);
              }

              return folderRepo
                  .nameExistInFolder(folderName, newParentId, requestContext.getUser().getId())
                  .map(
                      nameExistInFolder -> {
                        if (nameExistInFolder) {
                          throw new ServiceResponseException(
                              requestContext,
                              ErrorCode.FOLDER_WITH_NAME_ALREADY_EXISTS,
                              "Folder name already exists in new parent folder.",
                              400);
                        }
                        return Map.of(FolderRepo.PARENT_ID_COLUMN_NAME, newParentId);
                      });
            });
  }

  public Uni<ServiceResponse<Folder>> deleteFolder(
      final ServiceRequestContext requestContext, final long folderId) {
    return userService.checkUserExist(
        requestContext, context -> deleteIfFolderExists(context, folderId, true));
  }

  private Uni<ServiceResponse<Folder>> deleteIfFolderExists(
      final ServiceRequestContext requestContext,
      final long folderId,
      final boolean preventRootDelete) {
    return getFolderById(requestContext, folderId)
        .chain(
            folderServiceResponse -> {
              if (folderServiceResponse.hasError()) {
                return updateErrorAndPassThrough(folderServiceResponse);
              }

              Folder folderData = folderServiceResponse.getActionResponses().getFirst().getData();

              if (preventRootDelete && folderData.isRootFolder()) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                ResourceType.FOLDER,
                                ActionType.DELETE,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.ROOT_FOLDER_CANNOT_BE_DELETED)
                                        .errorMessage("Root folder cannot be deleted.")
                                        .httpCode(400)
                                        .build()))));
              }

              return getIdsToDelete(folderId, requestContext.getUser().getId())
                  .chain(toDelete -> deleteFolderInternal(requestContext, toDelete))
                  .map(
                      deleteError -> {
                        if (deleteError == null) {
                          return new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FOLDER, ActionType.DELETE, folderData));
                        } else {
                          return new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FOLDER, ActionType.DELETE, List.of(deleteError)));
                        }
                      });
            });
  }

  private Uni<Boolean> deleteFilesInternal(
      final ServiceRequestContext requestContext, final FoldersAndFilesToDelete toDelete) {
    List<Long> allFilesToDelete = toDelete.getFileIdsToDelete();
    if (allFilesToDelete.isEmpty()) {
      return Uni.createFrom().item(Boolean.TRUE);
    }

    List<List<Long>> filesToDeletePartitioned =
        Lists.partitionList(toDelete.getFileIdsToDelete(), config.bulkActions().maxDelete());

    ArrayList<Uni<ServiceResponse<File>>> fileDeleteUnis =
        new ArrayList<>(filesToDeletePartitioned.size());

    filesToDeletePartitioned.forEach(
        toDeleteList ->
            fileDeleteUnis.add(fileService.bulkDeleteFiles(requestContext, toDeleteList, false)));

    return Uni.combine()
        .all()
        .unis(fileDeleteUnis)
        .with(
            allServiceResponses -> {
              for (Object serviceResponse : allServiceResponses) {
                if (serviceResponse instanceof ServiceResponse<?> serviceResponseChecked) {
                  if (serviceResponseChecked.hasError()) {
                    return false;
                  }
                }
              }
              return true;
            });
  }

  private Uni<ServiceError> deleteFolderInternal(
      final ServiceRequestContext requestContext, final FoldersAndFilesToDelete toDelete) {

    Uni<Boolean> fileDeleteResult = deleteFilesInternal(requestContext, toDelete);

    return fileDeleteResult.chain(
        allDeleted -> {
          if (!allDeleted) {
            return Uni.createFrom()
                .item(
                    ServiceError.builder()
                        .errorCode(ErrorCode.FOLDER_DELETE_ERROR_FILES_COULD_NOT_BE_DELETED)
                        .errorMessage("Error deleting all the files in folder.")
                        .httpCode(400)
                        .build());
          }

          return folderRepo
              .deleteFolders(toDelete.getFolderIdsToDelete(), requestContext.getUser().getId())
              .map(
                  folderDeleteSuccess -> {
                    if (!folderDeleteSuccess) {
                      return ServiceError.builder()
                          .errorCode(ErrorCode.FOLDER_DELETE_ERROR)
                          .errorMessage("Could not delete all folders.")
                          .httpCode(400)
                          .build();
                    }

                    return null;
                  });
        });
  }

  private Uni<FoldersAndFilesToDelete> getIdsToDelete(
      final long folderIdToDelete, final long userId) {
    return getFolderHierarchy(userId)
        .map(hierarchy -> getFolderIdsToDelete(hierarchy, folderIdToDelete))
        .chain(
            folderIdsToDelete ->
                getFileIdsToDelete(folderIdsToDelete, userId)
                    .map(
                        fileIdsToDelete ->
                            new FoldersAndFilesToDelete(folderIdsToDelete, fileIdsToDelete)));
  }

  private Uni<List<Long>> getFileIdsToDelete(List<Long> folderIdsToDelete, final long userId) {
    return fileService
        .getFilesInFoldersInternal(folderIdsToDelete, userId)
        .map(
            filesToDelete -> {
              List<Long> fileIdsToDelete = new ArrayList<>(filesToDelete.size());
              filesToDelete.forEach(file -> fileIdsToDelete.add(file.getId()));
              return fileIdsToDelete;
            });
  }

  static List<Long> getFolderIdsToDelete(
      final FolderHierarchy folderHierarchy, final long folderToDelete) {
    FolderNode nodeToDelete = folderHierarchy.getNodeFromId(folderToDelete);
    List<Long> idsToDelete = new ArrayList<>();
    idsToDelete.add(folderToDelete);
    for (FolderNode childNode : nodeToDelete.getChildren()) {
      idsToDelete.addAll(getFolderIdsToDelete(folderHierarchy, childNode.getFolderId()));
    }
    return idsToDelete;
  }

  Uni<FolderHierarchy> getFolderHierarchy(final long userId) {
    return folderRepo.getAllUserFolders(userId).map(FolderHierarchy::of);
  }

  @Getter
  private static class FoldersAndFilesToDelete {
    private final List<Long> folderIdsToDelete;
    private final List<Long> fileIdsToDelete;

    public FoldersAndFilesToDelete(List<Long> folderIdsToDelete, List<Long> fileIdsToDelete) {
      this.folderIdsToDelete = folderIdsToDelete;
      this.fileIdsToDelete = fileIdsToDelete;
    }
  }
}
