package com.xnelo.filearch.common.service.storage;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.StorageType;
import io.smallrye.mutiny.Uni;
import java.io.InputStream;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public interface StorageService {
  StorageType getStorageType();

  Uni<ErrorCode> save(final FileUpload toUpload, final String key);

  Uni<ErrorCode> save(final byte[] toUpload, final String key);

  Uni<ErrorCode> delete(final String key);

  Uni<ErrorCode> bulkDelete(final List<String> keys);

  Uni<InputStream> getFileData(final String key);
}
