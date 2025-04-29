package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.common.model.mappers.FileMapper;
import com.xnelo.filearch.jooq.tables.StoredFiles;
import com.xnelo.filearch.jooq.tables.records.StoredFilesRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class StoredFilesRepo {
  private final DSLContext context;
  private final FileMapper mapper;

  @Inject
  public StoredFilesRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.mapper = Mappers.getMapper(FileMapper.class);
  }

  public Uni<File> createStoredFile(
      final long userId, final StorageType storageType, final String storageKey) {
    StoredFilesRecord toInsert = new StoredFilesRecord();
    toInsert.setOwnerUserId(userId);
    toInsert.setStorageType(storageType.getDbValue());
    toInsert.setStorageKey(storageKey);

    return Uni.createFrom()
        .item(context.insertInto(StoredFiles.STORED_FILES).set(toInsert).returning().fetchOne())
        .map(mapper::toFile);
  }
}
