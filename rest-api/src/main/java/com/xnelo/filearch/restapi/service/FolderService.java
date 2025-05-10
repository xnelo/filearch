package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.Folder;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.FolderContract;
import com.xnelo.filearch.restapi.data.FolderRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.function.Function;

@RequestScoped
public class FolderService {
  @Inject FolderRepo folderRepo;
  @Inject UserService userService;

  public Uni<Folder> createRootFolderForUser(final long userId) {
    return folderRepo.createRootFolder(userId);
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
}
