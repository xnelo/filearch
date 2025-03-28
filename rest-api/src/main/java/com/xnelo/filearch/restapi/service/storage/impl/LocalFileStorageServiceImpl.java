package com.xnelo.filearch.restapi.service.storage.impl;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.restapi.service.storage.StorageService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class LocalFileStorageServiceImpl implements StorageService {
  @Override
  public Uni<ErrorCode> save() {
    return Uni.createFrom().item(ErrorCode.OK);
  }
}
