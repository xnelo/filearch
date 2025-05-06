package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.Folder;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.restapi.data.FolderRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;

@RequestScoped
public class FolderService {
  @Inject FolderRepo folderRepo;

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
}
