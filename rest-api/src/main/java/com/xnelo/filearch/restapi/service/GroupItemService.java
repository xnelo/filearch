package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class GroupItemService {
  @Inject StoredFilesRepo storedFilesRepo;
  @Inject FolderRepo folderRepo;

  public Uni<Boolean> itemExists(
      final GroupItemType itemType, final long itemId, final long userId) {
    if (itemType == null) {
      log.warn("Invalid input: item type is null.");
      return Uni.createFrom().item(Boolean.FALSE);
    } else if (itemId < 0) {
      log.warn("Invalid input: itemId is negative.");
      return Uni.createFrom().item(Boolean.FALSE);
    }

    return switch (itemType) {
      case FILE -> fileItemExists(itemId, userId);
      case FOLDER -> folderItemExists(itemId, userId);
      case UNKNOWN -> {
        log.warn("Invalid input: itemType is unknown.");
        yield Uni.createFrom().item(Boolean.FALSE);
      }
    };
  }

  private Uni<Boolean> fileItemExists(final long fileId, final long userId) {
    return storedFilesRepo
        .getStoredFile(fileId, userId)
        .map(Objects::nonNull)
        .onFailure()
        .invoke(ex -> log.error("Error retrieving file({}) ", fileId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  private Uni<Boolean> folderItemExists(final long folderId, final long userId) {
    return folderRepo
        .getFolderById(folderId, userId)
        .map(Objects::nonNull)
        .onFailure()
        .invoke(ex -> log.error("Error retrieving folder({}) ", folderId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }
}
