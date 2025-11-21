package com.xnelo.filearch.common.messaging;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProcessFileRequest(
    long fileId,
    long ownerId,
    long folderId,
    String storageKey,
    String originalFilename,
    String mimeType) {}
