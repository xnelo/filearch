package com.xnelo.filearch.restapi.service.storage;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.StorageType;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public interface StorageService {
  StorageType getStorageType();

  Uni<ErrorCode> save(final FileUpload toUpload, final String key);
}
