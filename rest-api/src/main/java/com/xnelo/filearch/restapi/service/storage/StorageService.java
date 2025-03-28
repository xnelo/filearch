package com.xnelo.filearch.restapi.service.storage;

import com.xnelo.filearch.common.model.ErrorCode;
import io.smallrye.mutiny.Uni;

public interface StorageService {
  Uni<ErrorCode> save();
}
