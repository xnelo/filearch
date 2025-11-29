package com.xnelo.filearch.fileprocessorapi.contract;

public record FileMetadata(
    long fileId,
    long ownerId,
    long folderId,
    String storageKey,
    String originalFilename,
    String mimeType) {}
