package com.xnelo.filearch.common.data;

import com.xnelo.filearch.common.model.Artifact;
import com.xnelo.filearch.jooq.tables.Artifacts;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@RequestScoped
public class ArtifactRepo {
  private final DSLContext context;

  @Inject
  public ArtifactRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();
  }

  public Uni<Artifact> createArtifact(
      final long userId, final long fileId, final String storageKey, final String mimeType) {
    Map<String, Object> insertFields =
        Map.of(
            Artifacts.ARTIFACTS.OWNER_USER_ID.getName(),
            userId,
            Artifacts.ARTIFACTS.STORED_FILES_ID.getName(),
            fileId,
            Artifacts.ARTIFACTS.STORAGE_KEY.getName(),
            storageKey,
            Artifacts.ARTIFACTS.MIME_TYPE.getName(),
            mimeType);
    return Uni.createFrom()
        .item(
            context
                .insertInto(Artifacts.ARTIFACTS)
                .set(insertFields)
                .onConflictDoNothing()
                .returningResult(DSL.asterisk())
                .fetchOne())
        .map(this::toArtifactModel);
  }

  public Uni<List<Artifact>> getArtifactsByFileId(final long fileId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(DSL.asterisk())
                .from(Artifacts.ARTIFACTS)
                .where(Artifacts.ARTIFACTS.STORED_FILES_ID.eq(fileId))
                .and(Artifacts.ARTIFACTS.OWNER_USER_ID.eq(userId))
                .fetch()
                .map(this::toArtifactModel));
  }

  public Uni<Boolean> deleteArtifactsByFileId(final long fileId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(Artifacts.ARTIFACTS)
                .where(Artifacts.ARTIFACTS.STORED_FILES_ID.eq(fileId))
                .and(Artifacts.ARTIFACTS.OWNER_USER_ID.eq(userId))
                .execute())
        .map(numDeleted -> numDeleted > 0);
  }

  Artifact toArtifactModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return Artifact.builder()
        .id(toConvert.get(Artifacts.ARTIFACTS.ID))
        .ownerId(toConvert.get(Artifacts.ARTIFACTS.OWNER_USER_ID))
        .storedFileId(toConvert.get(Artifacts.ARTIFACTS.STORED_FILES_ID))
        .storageKey(toConvert.get(Artifacts.ARTIFACTS.STORAGE_KEY))
        .mimeType(toConvert.get(Artifacts.ARTIFACTS.MIME_TYPE))
        .build();
  }
}
