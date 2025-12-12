package com.xnelo.filearch.fileprocessor.processors;

import com.xnelo.filearch.common.data.ArtifactRepo;
import com.xnelo.filearch.common.model.Artifact;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.fileprocessorapi.contract.FileMetadata;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ProcessorBase {
  private final ArtifactRepo artifactRepo;
  private final StorageService storageService;

  @Inject
  public ProcessorBase(final ArtifactRepo artifactRepo, final StorageService storageService) {
    this.artifactRepo = artifactRepo;
    this.storageService = storageService;
  }

  @ActivateRequestContext
  protected Artifact storeArtifact(
      final byte[] toUpload,
      final FileMetadata fileMetadata,
      final String artifactExtension,
      final String mimeType) {
    log.info(
        "Storing artifact for fileId={} artifactExtension={} mimeType={}",
        fileMetadata.fileId(),
        artifactExtension,
        mimeType);
    String storageKey = fileMetadata.storageKey() + "." + artifactExtension;
    ErrorCode errorCode = storageService.save(toUpload, storageKey).await().indefinitely();
    log.info("Storage error code = {}", errorCode);
    if (errorCode != ErrorCode.OK) {
      log.error("Error storing file. errorCode={}", errorCode);
      return null;
    }

    Artifact artifact =
        artifactRepo
            .createArtifact(fileMetadata.ownerId(), fileMetadata.fileId(), storageKey, mimeType)
            .await()
            .indefinitely();
    log.info("Artifact Stored artifactId={}", artifact.getId());
    return artifact;
  }
}
