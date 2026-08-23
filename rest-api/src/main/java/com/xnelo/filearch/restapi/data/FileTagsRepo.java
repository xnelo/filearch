package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.exception.RepoException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.jooq.tables.FileTags;
import com.xnelo.filearch.jooq.tables.records.FileTagsRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@RequestScoped
@Slf4j
public class FileTagsRepo {
  private final DSLContext context;

  @Inject
  public FileTagsRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();
  }

  public Uni<Boolean> deleteAllTagUses(final long tagId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(FileTags.FILE_TAGS)
                .where(FileTags.FILE_TAGS.TAG_ID.eq(tagId))
                .execute())
        .map(recordsDeleted -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting tag usages '{}'", tagId, ex))
        .onFailure()
        .transform(
            ex ->
                new RepoException(
                    ErrorCode.TAG_USES_COULD_NOT_BE_DELETED,
                    "Tag(" + tagId + ") uses could not be deleted."));
  }

  public Uni<Boolean> deleteAllFileMappings(final long fileId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(FileTags.FILE_TAGS)
                .where(FileTags.FILE_TAGS.FILE_ID.eq(fileId))
                .execute())
        .map(recordsDeleted -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting file mappings '{}'", fileId, ex))
        .onFailure()
        .transform(
            ex ->
                new RepoException(
                    ErrorCode.FILE_TAG_MAPPING_UNABLE_TO_DELETE,
                    "Unable to delete Tile Tag Mapping '" + fileId + "'."));
  }

  public Uni<Boolean> assignFileMapping(final long fileId, final long tagId, final Long groupId) {
    InsertSetMoreStep<FileTagsRecord> insertStatement =
        context
            .insertInto(FileTags.FILE_TAGS)
            .set(FileTags.FILE_TAGS.FILE_ID, fileId)
            .set(FileTags.FILE_TAGS.TAG_ID, tagId);

    if (groupId != null) {
      insertStatement = insertStatement.set(FileTags.FILE_TAGS.GROUP_ID, groupId);
    }

    return Uni.createFrom()
        .item(insertStatement.onConflictDoNothing().execute())
        .map(res -> res == 1)
        .onFailure()
        .invoke(ex -> log.error("Error inserting file tag mapping", ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> unassignFileMapping(final long fileId, final long tagId, final Long groupId) {
    DeleteConditionStep<FileTagsRecord> deleteStatement =
        context
            .deleteFrom(FileTags.FILE_TAGS)
            .where(FileTags.FILE_TAGS.FILE_ID.eq(fileId))
            .and(FileTags.FILE_TAGS.TAG_ID.eq(tagId));

    if (groupId == null) {
      deleteStatement = deleteStatement.and(FileTags.FILE_TAGS.GROUP_ID.isNull());
    } else {
      deleteStatement = deleteStatement.and(FileTags.FILE_TAGS.GROUP_ID.eq(groupId));
    }

    return Uni.createFrom()
        .item(deleteStatement.execute())
        .map(res -> res == 1)
        .onFailure()
        .invoke(ex -> log.error("Error deleting a file tag mapping", ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }
}
