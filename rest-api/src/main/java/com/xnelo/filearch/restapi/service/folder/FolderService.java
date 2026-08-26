package com.xnelo.filearch.restapi.service.folder;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.*;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
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
import lombok.Getter;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class FolderService {
  @Inject FolderRepo folderRepo;
  @Inject UserService userService;
  @Inject FileService fileService;
  @Inject FilearchConfig config;
  final PaginationMapper paginationMapper = Mappers.getMapper(PaginationMapper.class);

  public static final String FOLDER_INFO_KEY = "FOLDER_INFO__FOLDER";

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

    return userService
        .checkUserExist(requestContext)
        .chain(context -> folderRepo.getAll(context.getUser().getId(), paginationParameters))
        .map(
            paginatedFolders ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        paginationMapper.toPaginatedResponse(paginatedFolders))));
  }

  public Uni<ServiceResponse<PaginatedResponse<File>>> getAllFilesInFolder(
      final ServiceRequestContext requestContext,
      final Long folderId,
      final PaginationParameters paginationParameters) {
    Utils.validatePaginationParameters(requestContext, paginationParameters);

    return userService
        .checkUserExist(requestContext)
        .chain(context -> checkFolderExist(context, folderId))
        .chain(context -> fileService.getAllFilesInFolder(context, folderId, paginationParameters));
  }

  public Uni<ServiceResponse<List<Long>>> getAllFileIdsInFolder(
      final ServiceRequestContext requestContext, final long folderId) {
    return fileService.getAllFileIdsInFolder(requestContext, folderId);
  }

  public Uni<ServiceRequestContext> checkFolderExist(
      ServiceRequestContext context, final long folderId) {
    Utils.checkUserInRequest(context);

    return folderRepo
        .getFolderById(folderId, context.getUser().getId())
        .map(
            folderInfo -> {
              if (folderInfo == null) {
                throw new ServiceResponseException(
                    context,
                    ErrorCode.FOLDER_DOES_NOT_EXIST,
                    "Folder (" + folderId + ") does not exist.",
                    404);
              }

              context.setData(FOLDER_INFO_KEY, folderInfo);

              return context;
            });
  }

  Uni<ServiceRequestContext> checkIfNameInFolder(
      ServiceRequestContext context, final String nameToCheck, final long folderInId) {
    Utils.checkUserInRequest(context);

    return folderRepo
        .nameExistInFolder(nameToCheck, folderInId, context.getUser().getId())
        .map(
            nameExists -> {
              if (nameExists) {
                throw new ServiceResponseException(
                    context,
                    ErrorCode.FOLDER_WITH_NAME_ALREADY_EXISTS,
                    "A folder with the name '" + nameToCheck + "' already exists.",
                    400);
              }

              return context;
            });
  }

  public Uni<ServiceResponse<Folder>> createNewFolder(
      final ServiceRequestContext requestContext, final FolderContract newFolder) {
    return userService
        .checkUserExist(requestContext)
        .chain(context -> checkFolderExist(context, newFolder.getParentId()))
        .chain(
            context ->
                checkIfNameInFolder(context, newFolder.getFolderName(), newFolder.getParentId()))
        .chain(
            context ->
                folderRepo.createFolder(
                    context.getUser().getId(), newFolder.getParentId(), newFolder.getFolderName()))
        .map(
            newlyCreatedFolder ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        newlyCreatedFolder)));
  }

  public Uni<ServiceResponse<Folder>> getFolderById(
      final ServiceRequestContext requestContext, final long folderId) {
    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> folderRepo.getFolderById(folderId, context2.getUser().getId()))
        .map(
            folder -> {
              if (folder == null) {
                throw new ServiceResponseException(
                    requestContext, ErrorCode.FOLDER_DOES_NOT_EXIST, "Folder does not exist.", 404);
              }

              return new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(), requestContext.getActionType(), folder));
            });
  }

  public Uni<ServiceResponse<Folder>> updateFolder(
      final ServiceRequestContext requestContext,
      final long folderId,
      final FolderContract folderUpdateData) {
    if (folderUpdateData.getId() != null) {
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.FOLDER_ID_CANNOT_BE_UPDATED,
          "Folder id cannot be updated.",
          400);
    } else if (folderUpdateData.getOwnerId() != null) {
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.FOLDER_OWNER_CANNOT_BE_UPDATED,
          "Folder Owner cannot be updated.",
          400);
    }

    return userService
        .checkUserExist(requestContext)
        .chain(context2 -> folderRepo.getFolderById(folderId, context2.getUser().getId()))
        .chain(
            folderToUpdateMetadata ->
                generateUpdateMap(requestContext, folderToUpdateMetadata, folderUpdateData))
        .chain(folderUpdateMap -> doFolderUpdates(requestContext, folderUpdateMap, folderId))
        .map(
            updatedFolder ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        updatedFolder)));
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

  private Uni<Folder> doFolderUpdates(
      final ServiceRequestContext requestContext,
      final Map<String, Object> folderUpdateMap,
      final long folderId) {
    if (folderUpdateMap.isEmpty()) {
      throw new ServiceResponseException(
          requestContext, ErrorCode.NO_FIELDS_TO_UPDATE, "No Fields to update.", 400);
    }

    return folderRepo.updateFolder(folderId, requestContext.getUser().getId(), folderUpdateMap);
  }

  private Uni<Map<String, Object>> generateUpdateMap(
      final ServiceRequestContext requestContext,
      final Folder folderToUpdateMetadata,
      final FolderContract folderUpdateData) {
    if (folderToUpdateMetadata == null) {
      throw new ServiceResponseException(
          requestContext, ErrorCode.FOLDER_DOES_NOT_EXIST, "Folder does not exist.", 404);
    }

    // Check if this is the root folder
    if (folderToUpdateMetadata.isRootFolder()) {
      throw new ServiceResponseException(
          requestContext,
          ErrorCode.ROOT_FOLDER_CANNOT_BE_UPDATED,
          "Root Folder cannot be updated.",
          400);
    }

    ArrayList<Uni<Map<String, Object>>> checkUnis = new ArrayList<>();
    long destFolderId = folderToUpdateMetadata.getId();
    if (folderUpdateData.getParentId() != null) {
      checkUnis.add(
          updateFolderLocation(
              requestContext,
              folderToUpdateMetadata.getFolderName(),
              folderUpdateData.getParentId()));
      destFolderId = folderUpdateData.getParentId();
    }

    if (folderUpdateData.getFolderName() != null) {
      checkUnis.add(
          updateFolderName(requestContext, destFolderId, folderUpdateData.getFolderName()));
    }

    return Uni.combine().all().unis(checkUnis).with(this::combineUpdateChecks);
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
    return userService
        .checkUserExist(requestContext)
        .chain(context -> deleteIfFolderExists(context, folderId, true));
  }

  private Uni<ServiceResponse<Folder>> deleteIfFolderExists(
      final ServiceRequestContext requestContext,
      final long folderId,
      final boolean preventRootDelete) {
    return checkFolderExist(requestContext, folderId)
        .chain(
            context -> {
              Folder folderData = context.getDataAs(FOLDER_INFO_KEY, Folder.class);
              if (preventRootDelete && folderData.isRootFolder()) {
                throw new ServiceResponseException(
                    context,
                    ErrorCode.ROOT_FOLDER_CANNOT_BE_DELETED,
                    "Root folder cannot be deleted.",
                    400);
              }

              return getIdsToDelete(folderId, context.getUser().getId());
            })
        .chain(toDelete -> deleteFolderInternal(requestContext, toDelete))
        .map(
            _ignore ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        ResourceType.FOLDER,
                        ActionType.DELETE,
                        requestContext.getDataAs(FOLDER_INFO_KEY, Folder.class))));
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

  private Uni<Boolean> deleteFolderInternal(
      final ServiceRequestContext requestContext, final FoldersAndFilesToDelete toDelete) {

    return deleteFilesInternal(requestContext, toDelete)
        .invoke(
            allDeleted -> {
              if (!allDeleted) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.FOLDER_DELETE_ERROR_FILES_COULD_NOT_BE_DELETED,
                    "Error deleting all the files in folder.",
                    500);
              }
            })
        .chain(
            _ignored ->
                folderRepo.deleteFolders(
                    toDelete.getFolderIdsToDelete(), requestContext.getUser().getId()))
        .invoke(
            folderDeleteSuccess -> {
              if (!folderDeleteSuccess) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.FOLDER_DELETE_ERROR,
                    "Could not delete all folders.",
                    500);
              }
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

  @SuppressWarnings("ClassCanBeRecord")
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
