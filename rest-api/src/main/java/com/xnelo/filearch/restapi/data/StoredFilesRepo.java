package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.SortDirection;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.jooq.tables.StoredFiles;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

@RequestScoped
public class StoredFilesRepo {
  public static final String DECRYPTED_ORIGINAL_FILENAME = "DECRYPT_ORIGINAL_FILENAME";

  private final DSLContext context;
  private final String encryptionKey;
  private final List<? extends SelectField<?>> allFields;

  @Inject
  public StoredFilesRepo(
      final AgroalDataSource dataSource,
      @ConfigProperty(name = "filearch.encryption-key", defaultValue = "LOCAL_DEV_ENCRYPTION_KEY")
          final String encryptionKey) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();

    this.encryptionKey = encryptionKey;

    this.allFields =
        List.of(
            StoredFiles.STORED_FILES.ID,
            StoredFiles.STORED_FILES.OWNER_USER_ID,
            StoredFiles.STORED_FILES.FOLDER_ID,
            StoredFiles.STORED_FILES.STORAGE_TYPE,
            StoredFiles.STORED_FILES.STORAGE_KEY,
            decryptField(StoredFiles.STORED_FILES.ORIGINAL_FILENAME, encryptionKey)
                .as(DECRYPTED_ORIGINAL_FILENAME));
  }

  public Uni<PaginatedData<File>> getAll(
      final long userId, final Long after, final Integer limit, final SortDirection sortDirection) {
    SelectConditionStep<?> selectStatement =
        context
            .select(allFields)
            .from(StoredFiles.STORED_FILES)
            .where(StoredFiles.STORED_FILES.OWNER_USER_ID.eq(userId));

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(
            selectStatement, StoredFiles.STORED_FILES.ID, after, limit, sortDirection);

    List<File> data = finalQuery.fetch().map(this::toFileModel);

    return Uni.createFrom().item(RepoUtils.toPaginatedData(after, data, sortDirection, limit));
  }

  public Uni<File> createStoredFile(
      final long userId,
      final long folderId,
      final StorageType storageType,
      final String storageKey,
      final String originalFilename) {
    Map<String, Object> insertFields =
        Map.of(
            StoredFiles.STORED_FILES.OWNER_USER_ID.getName(),
            userId,
            StoredFiles.STORED_FILES.FOLDER_ID.getName(),
            folderId,
            StoredFiles.STORED_FILES.STORAGE_TYPE.getName(),
            storageType.getDbValue(),
            StoredFiles.STORED_FILES.STORAGE_KEY.getName(),
            storageKey,
            StoredFiles.STORED_FILES.ORIGINAL_FILENAME.getName(),
            encryptField(originalFilename, encryptionKey));

    return Uni.createFrom()
        .item(
            context
                .insertInto(StoredFiles.STORED_FILES)
                .set(insertFields)
                .returningResult(allFields)
                .fetchOne())
        .map(this::toFileModel);
  }

  public Uni<File> getStoredFile(final long fileId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(StoredFiles.STORED_FILES)
                .where(
                    StoredFiles.STORED_FILES
                        .ID
                        .eq(fileId)
                        .and(StoredFiles.STORED_FILES.OWNER_USER_ID.eq(userId)))
                .fetchOne())
        .map(this::toFileModel);
  }

  public Uni<Boolean> deleteStoredFile(final long fileId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(StoredFiles.STORED_FILES)
                .where(
                    StoredFiles.STORED_FILES
                        .ID
                        .eq(fileId)
                        .and(StoredFiles.STORED_FILES.OWNER_USER_ID.eq(userId)))
                .execute())
        .map(recordsDeleted -> recordsDeleted == 1);
  }

  public Uni<List<File>> getFilesInFolders(final List<Long> folderIds, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(StoredFiles.STORED_FILES)
                .where(StoredFiles.STORED_FILES.OWNER_USER_ID.eq(userId))
                .and(StoredFiles.STORED_FILES.FOLDER_ID.in(folderIds))
                .fetch()
                .map(this::toFileModel));
  }

  public Uni<PaginatedData<File>> getFilesInFolder(
      final Long folderId,
      final long userId,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    SelectConditionStep<?> selectStatement =
        context
            .select(allFields)
            .from(StoredFiles.STORED_FILES)
            .where(StoredFiles.STORED_FILES.OWNER_USER_ID.eq(userId))
            .and(StoredFiles.STORED_FILES.FOLDER_ID.eq(folderId));

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(
            selectStatement, StoredFiles.STORED_FILES.ID, after, limit, sortDirection);

    List<File> data = finalQuery.fetch().map(this::toFileModel);

    return Uni.createFrom().item(RepoUtils.toPaginatedData(after, data, sortDirection, limit));
  }

  File toFileModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return File.builder()
        .id(toConvert.get(StoredFiles.STORED_FILES.ID))
        .ownerId(toConvert.get(StoredFiles.STORED_FILES.OWNER_USER_ID))
        .folderId(toConvert.get(StoredFiles.STORED_FILES.FOLDER_ID))
        .storageType(StorageType.fromString(toConvert.get(StoredFiles.STORED_FILES.STORAGE_TYPE)))
        .storageKey(toConvert.get(StoredFiles.STORED_FILES.STORAGE_KEY))
        .originalFilename(toConvert.get(DECRYPTED_ORIGINAL_FILENAME, String.class))
        .build();
  }
}
