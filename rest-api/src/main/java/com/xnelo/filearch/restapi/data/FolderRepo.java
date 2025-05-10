package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.Folder;
import com.xnelo.filearch.jooq.tables.Folders;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectField;
import org.jooq.impl.DSL;

@RequestScoped
public class FolderRepo {
  public static final String DECRYPTED_NAME = "DECRYPT_NAME";

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
