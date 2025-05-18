package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.Folder;
import com.xnelo.filearch.common.model.SortDirection;
import com.xnelo.filearch.jooq.tables.Folders;
import io.agroal.api.AgroalDataSource;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

@RequestScoped
public class FolderRepo {
  public static final String FOLDER_NAME_COLUMN_NAME = Folders.FOLDERS.NAME.getName();
  public static final String PARENT_ID_COLUMN_NAME = Folders.FOLDERS.PARENT_ID.getName();
  public static final String DECRYPTED_NAME = "DECRYPT_NAME";

  public static final List<String> FIELDS_TO_ENCRYPT = List.of(FOLDER_NAME_COLUMN_NAME);

  private final DSLContext context;
  private final String encryptionKey;
  private final List<? extends SelectField<?>> allFields;

  @Inject
  public FolderRepo(
      final AgroalDataSource dataSource,
      @ConfigProperty(name = "filearch.encryption-key", defaultValue = "LOCAL_DEV_ENCRYPTION_KEY")
          final String encryptionKey) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();

    this.encryptionKey = encryptionKey;

    this.allFields =
        List.of(
            Folders.FOLDERS.ID,
            Folders.FOLDERS.OWNER_USER_ID,
            Folders.FOLDERS.PARENT_ID,
            decryptField(Folders.FOLDERS.NAME, encryptionKey).as(DECRYPTED_NAME));
  }

  public Uni<List<Folder>> getAll(
      final long userId, final Long after, final Integer limit, final SortDirection sortDirection) {
    SelectConditionStep<?> selectStatement =
        context
            .select(allFields)
            .from(Folders.FOLDERS)
            .where(Folders.FOLDERS.OWNER_USER_ID.eq(userId));

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(selectStatement, Folders.FOLDERS.ID, after, limit, sortDirection);

    return Uni.createFrom().item(finalQuery.fetch().map(this::toFolderModel));
  }

  public Uni<Folder> createRootFolder(final long userId) {
    return Uni.createFrom()
        .item(
            context
                .insertInto(Folders.FOLDERS)
                .set(Folders.FOLDERS.OWNER_USER_ID, userId)
                .returningResult(allFields)
                .fetchOne())
        .map(this::toFolderModel);
  }

  public Uni<Folder> createFolder(final long userId, final long parentId, final String name) {
    Map<String, Object> insertFields =
        Map.of(
            Folders.FOLDERS.OWNER_USER_ID.getName(),
            userId,
            Folders.FOLDERS.PARENT_ID.getName(),
            parentId,
            Folders.FOLDERS.NAME.getName(),
            encryptField(name, encryptionKey));
    return Uni.createFrom()
        .item(
            context
                .insertInto(Folders.FOLDERS)
                .set(insertFields)
                .onConflictDoNothing()
                .returningResult(allFields)
                .fetchOne())
        .map(this::toFolderModel);
  }

  public Uni<Folder> getFolderById(final long folderId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(Folders.FOLDERS)
                .where(
                    Folders.FOLDERS.ID.eq(folderId).and(Folders.FOLDERS.OWNER_USER_ID.eq(userId)))
                .fetchOne())
        .map(this::toFolderModel);
  }

  public Uni<Boolean> nameExistInFolder(
      final String nameToCheck, final long folderId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(DSL.count().as("number_with_name"))
                .from(Folders.FOLDERS)
                .where(Folders.FOLDERS.OWNER_USER_ID.eq(userId))
                .and(Folders.FOLDERS.PARENT_ID.eq(folderId))
                .and(decryptField(Folders.FOLDERS.NAME, encryptionKey).eq(nameToCheck))
                .fetchOne())
        .map(dbRecord -> dbRecord.getValue(0, Integer.class) > 0);
  }

  public Uni<Folder> updateFolder(
      final long folderId, final long userId, Map<String, Object> updateFields) {
    Map<String, Object> encryptedUpdateFields = encryptFields(updateFields);
    return Uni.createFrom()
        .item(
            context
                .update(Folders.FOLDERS)
                .set(encryptedUpdateFields)
                .where(
                    Folders.FOLDERS.ID.eq(folderId).and(Folders.FOLDERS.OWNER_USER_ID.eq(userId)))
                .returningResult(allFields)
                .fetchOne())
        .map(this::toFolderModel);
  }

  Map<String, Object> encryptFields(Map<String, Object> fieldsMap) {
    Map<String, Object> returnMap = new HashMap<>(fieldsMap.size());

    fieldsMap.forEach(
        (key, value) -> {
          if (FIELDS_TO_ENCRYPT.contains(key)) {
            if (value instanceof String stringValue) {
              returnMap.put(key, encryptField(stringValue, encryptionKey));
            } else {
              Log.errorf(
                  "Error encrypting field. Value was not a string. fieldname=%s type=%s",
                  key, value.getClass().getName());
              throw new RuntimeException("ERROR ENCRYPTING FIELD.");
            }
          } else {
            returnMap.put(key, value);
          }
        });

    return returnMap;
  }

  public Uni<List<Folder>> getAllUserFolders(final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(Folders.FOLDERS)
                .where(Folders.FOLDERS.OWNER_USER_ID.eq(userId))
                .fetch()
                .map(this::toFolderModel));
  }

  public Uni<Boolean> deleteFolders(final Collection<Long> folderIdsToDelete, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(Folders.FOLDERS)
                .where(Folders.FOLDERS.OWNER_USER_ID.eq(userId))
                .and(Folders.FOLDERS.ID.in(folderIdsToDelete))
                .execute())
        .map(foldersDeleted -> foldersDeleted == folderIdsToDelete.size());
  }

  Folder toFolderModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return Folder.builder()
        .id(toConvert.get(Folders.FOLDERS.ID))
        .ownerId(toConvert.get(Folders.FOLDERS.OWNER_USER_ID))
        .parentId(toConvert.get(Folders.FOLDERS.PARENT_ID))
        .folderName(toConvert.get(DECRYPTED_NAME, String.class))
        .build();
  }
}
