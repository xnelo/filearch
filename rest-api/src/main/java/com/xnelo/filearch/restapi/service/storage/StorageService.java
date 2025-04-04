package com.xnelo.filearch.restapi.service.storage;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.restapi.api.contracts.UploadFileResource;
import io.smallrye.mutiny.Uni;

public interface StorageService {
  Uni<ErrorCode> save(UploadFileResource toUpload, String key);
}
