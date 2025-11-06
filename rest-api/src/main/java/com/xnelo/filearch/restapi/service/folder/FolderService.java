package com.xnelo.filearch.restapi.service.folder;

import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.utils.Lists;
import com.xnelo.filearch.restapi.api.contracts.FolderContract;
import com.xnelo.filearch.restapi.api.mappers.PaginationMapper;
import com.xnelo.filearch.restapi.config.FilearchConfig;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.service.FileService;
import com.xnelo.filearch.restapi.service.UserService;
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

  public Uni<ServiceResponse<Folder>> deleteRootFolder(final long folderId, final long userId) {
    return deleteIfFolderExists(folderId, userId, false);
  }

  public Uni<ServiceResponse<PaginatedResponse<Folder>>> getAllFolders(
      final UserToken userInfo,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    if (after != null && after < 0) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FOLDER,
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
                                ResourceType.FOLDER,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage("User does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return folderRepo
                  .getAll(user.getId(), after, limit, sortDirection)
                  .map(
                      paginatedFolders ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  ResourceType.FOLDER,
                                  ActionType.GET,
                                  paginationMapper.toPaginatedResponse(paginatedFolders))));
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
                      ResourceType.FOLDER,
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
                                ResourceType.FOLDER,
                                ActionType.GET,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage("User does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return checkFolderExist(
                  folderId,
                  user.getId(),
                  ResourceType.FOLDER,
                  ActionType.GET,
                  folder ->
                      fileService.getAllFilesInFolder(
                          userInfo, folderId, after, limit, sortDirection));
            });
  }

  public Uni<ServiceResponse<Folder>> getFolderById(final long folderId, final long userId) {
    return folderRepo
        .getFolderById(folderId, userId)
        .map(
            folder -> {
              if (folder == null) {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        ResourceType.FOLDER,
                        ActionType.GET,
                        List.of(
                            ServiceError.builder()
                                .httpCode(404)
                                .errorCode(ErrorCode.FOLDER_DOES_NOT_EXIST)
                                .errorMessage("Folder does not exist.")
                                .build())));
              }

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(ResourceType.FOLDER, ActionType.GET, folder));
            });
  }

  public <T> Uni<ServiceResponse<T>> checkFolderExist(
      final long folderId,
      final long userId,
      final ResourceType resourceType,
      final ActionType actionType,
      final Function<Folder, Uni<ServiceResponse<T>>> folderExistAction) {
    return getFolderById(folderId, userId)
        .chain(
            folderResponse -> {
              Folder folder = folderResponse.getActionResponses().getFirst().getData();
              if (folder == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                resourceType,
                                actionType,
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.FOLDER_DOES_NOT_EXIST)
                                        .errorMessage(
                                            "Cannot "
                                                + actionType
                                                + " "
                                                + resourceType
                                                + " because folder does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              return folderExistAction.apply(folder);
            });
  }

  public Uni<ServiceResponse<Folder>> createNewFolder(
      final FolderContract newFolder, final UserToken userToken) {
    return userService.checkUserExist(
        userToken,
        ResourceType.FOLDER,
        ActionType.CREATE,
        user ->
            checkFolderExist(
                newFolder.getParentId(),
                user.getId(),
                ResourceType.FOLDER,
                ActionType.CREATE,
                parentFolder ->
                    folderRepo
                        .nameExistInFolder(
                            newFolder.getFolderName(), newFolder.getParentId(), user.getId())
                        .chain(
                            nameExists -> {
                              if (nameExists) {
                                return Uni.createFrom()
                                    .item(
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                ResourceType.FOLDER,
                                                ActionType.CREATE,
                                                List.of(
                                                    ServiceError.builder()
                                                        .httpCode(400)
                                                        .errorCode(
                                                            ErrorCode
                                                                .FOLDER_WITH_NAME_ALREADY_EXISTS)
                                                        .errorMessage(
                                                            "A folder with the name '"
                                                                + newFolder.getFolderName()
                                                                + "' already exists.")
                                                        .build()))));
                              }

                              return folderRepo
                                  .createFolder(
                                      user.getId(),
                                      newFolder.getParentId(),
                                      newFolder.getFolderName())
                                  .map(
                                      newlyCreatedFolder ->
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  ResourceType.FOLDER,
                                                  ActionType.CREATE,
                                                  newlyCreatedFolder)));
                            })));
  }

  public Uni<ServiceResponse<Folder>> getFolderById(final long folderId, final UserToken userInfo) {
    return userService.checkUserExist(
        userInfo,
        ResourceType.FOLDER,
        ActionType.GET,
        user -> getFolderById(folderId, user.getId()));
  }

  @SuppressWarnings("unchecked")
  public Uni<ServiceResponse<Folder>> updateFolder(
      final long folderId, final UserToken userInfo, final FolderContract folderData) {
    if (folderData.getId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FOLDER,
                      ActionType.UPDATE,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.FOLDER_ID_CANNOT_BE_UPDATED)
                              .errorMessage("Folder id cannot be updated.")
                              .httpCode(400)
                              .build()))));
    } else if (folderData.getOwnerId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      ResourceType.FOLDER,
                      ActionType.UPDATE,
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.FOLDER_OWNER_CANNOT_BE_UPDATED)
                              .errorMessage("Folder Owner cannot be updated.")
                              .httpCode(400)
                              .build()))));
    }

    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userResponse -> {
              if (userResponse.hasError()) {
                return updateErrorAndPassThrough(userResponse);
              }
              User user = userResponse.getActionResponses().getFirst().getData();
              return getFolderById(folderId, user.getId())
                  .chain(
                      folderResponse -> {
                        if (folderResponse.hasError()) {
                          return updateErrorAndPassThrough(folderResponse);
                        }
                        Folder folderMetadata =
                            folderResponse.getActionResponses().getFirst().getData();
                        ArrayList<Uni<Map<String, Object>>> checkUnis = new ArrayList<>();
                        long destFolderId = folderId;
                        if (folderData.getParentId() != null) {
                          checkUnis.add(
                              updateFolderLocation(
                                  folderMetadata.getFolderName(),
                                  user.getId(),
                                  folderData.getParentId()));
                          destFolderId = folderData.getParentId();
                        }

                        if (folderData.getFolderName() != null) {
                          checkUnis.add(
                              updateFolderName(
                                  destFolderId, user.getId(), folderData.getFolderName()));
                        }

                        Uni<Map<String, Object>> folderUpdates =
                            Uni.combine().all().unis(checkUnis).with(this::combineUpdateChecks);

                        return folderUpdates.chain(
                            folderUpdateMap -> {
                              if (folderUpdateMap.isEmpty()) {
                                return Uni.createFrom()
                                    .item(
                                        new ServiceResponse<>(
                                            new ServiceActionResponse<>(
                                                ResourceType.FOLDER,
                                                ActionType.UPDATE,
                                                List.of(
                                                    ServiceError.builder()
                                                        .errorCode(ErrorCode.NO_FIELDS_TO_UPDATE)
                                                        .errorMessage("No Fields to update.")
                                                        .httpCode(400)
                                                        .build()))));
                              }

                              return folderRepo
                                  .updateFolder(folderId, user.getId(), folderUpdateMap)
                                  .map(
                                      updatedFolder ->
                                          new ServiceResponse<>(
                                              new ServiceActionResponse<>(
                                                  ResourceType.FOLDER,
                                                  ActionType.UPDATE,
                                                  updatedFolder)));
                            });
                      });
            })
        .onFailure(FolderServiceException.class)
        .recoverWithItem(
            ex -> {
              FolderServiceException folderException = (FolderServiceException) ex;
              return new ServiceResponse<Folder>(
                  new ServiceActionResponse<>(
                      ResourceType.FOLDER,
                      folderException.getActionType(),
                      List.of(folderException.getError())));
            })
        .map(folderServiceResponse -> (ServiceResponse<Folder>) folderServiceResponse);
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
      final long folderId, final long userId, final String newFolderName) {
    // Preconditions:
    // * The folder to move exists
    // * The user exists

    return folderRepo
        .nameExistInFolder(newFolderName, folderId, userId)
        .map(
            folderNameExists -> {
              if (folderNameExists) {
                throw new FolderServiceException(
                    ActionType.UPDATE,
                    ServiceError.builder()
                        .errorCode(ErrorCode.FOLDER_WITH_NAME_ALREADY_EXISTS)
                        .errorMessage("Folder already has another folder named " + newFolderName)
                        .httpCode(400)
                        .build());
              }
              return Map.of(FolderRepo.FOLDER_NAME_COLUMN_NAME, newFolderName);
            });
  }

  private Uni<Map<String, Object>> updateFolderLocation(
      final String folderName, final long userId, final long newParentId) {
    // Preconditions:
    // * The folder to move exists
    // * The user exists

    return getFolderById(newParentId, userId)
        .chain(
            folderResponse -> {
              if (folderResponse.hasError()) {
                throw new FolderServiceException(
                    ActionType.UPDATE,
                    ServiceError.builder()
                        .errorCode(ErrorCode.FOLDER_DOES_NOT_EXIST)
                        .errorMessage("New Parent folder does not exist.")
                        .httpCode(404)
                        .build());
              }

              return folderRepo
                  .nameExistInFolder(folderName, newParentId, userId)
                  .map(
                      nameExistInFolder -> {
                        if (nameExistInFolder) {
                          throw new FolderServiceException(
                              ActionType.UPDATE,
                              ServiceError.builder()
                                  .errorCode(ErrorCode.FOLDER_WITH_NAME_ALREADY_EXISTS)
                                  .errorMessage("Folder name already exists in new parent folder.")
                                  .httpCode(400)
                                  .build());
                        }
                        return Map.of(FolderRepo.PARENT_ID_COLUMN_NAME, newParentId);
                      });
            });
  }

  public Uni<ServiceResponse<Folder>> deleteFolder(final long folderId, final UserToken userInfo) {
    return userService
        .getUserFromUserToken(userInfo)
        .chain(
            userServiceResponse -> {
              if (userServiceResponse.hasError()) {
                return updateErrorAndPassThrough(userServiceResponse);
              }

              User user = userServiceResponse.getActionResponses().getFirst().getData();

              return deleteIfFolderExists(folderId, user.getId(), true);
            });
  }

  private Uni<ServiceResponse<Folder>> deleteIfFolderExists(
      final long folderId, final long userId, final boolean preventRootDelete) {
    return getFolderById(folderId, userId)
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

              return getIdsToDelete(folderId, userId)
                  .chain(toDelete -> deleteFolderInternal(toDelete, userId))
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
      final FoldersAndFilesToDelete toDelete, final long userId) {
    List<Long> allFilesToDelete = toDelete.getFileIdsToDelete();
    if (allFilesToDelete.isEmpty()) {
      return Uni.createFrom().item(Boolean.TRUE);
    }

    List<List<Long>> filesToDeletePartitioned =
        Lists.partitionList(toDelete.getFileIdsToDelete(), config.bulkActions().maxDelete());

    ArrayList<Uni<ServiceResponse<File>>> fileDeleteUnis =
        new ArrayList<>(filesToDeletePartitioned.size());

    filesToDeletePartitioned.forEach(
        toDeleteList -> fileDeleteUnis.add(fileService.bulkDeleteFiles(toDeleteList, userId)));

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
      final FoldersAndFilesToDelete toDelete, final long userId) {

    Uni<Boolean> fileDeleteResult = deleteFilesInternal(toDelete, userId);

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
              .deleteFolders(toDelete.getFolderIdsToDelete(), userId)
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
  private static class FolderServiceException extends RuntimeException {
    private final ActionType actionType;
    private final ServiceError error;

    public FolderServiceException(ActionType at, ServiceError error) {
      actionType = at;
      this.error = error;
    }
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
