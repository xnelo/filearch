package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.SortDirection;
import com.xnelo.filearch.common.model.Tag;
import com.xnelo.filearch.jooq.tables.FileTags;
import com.xnelo.filearch.jooq.tables.Tags;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

@Slf4j
@RequestScoped
public class TagRepo {
  private final int DEFAULT_SEARCH_LIMIT = 50;
  public static final String DECRYPTED_TAG_NAME = "DECRYPT_TAG_NAME";

  private final DSLContext context;
  private final String encryptionKey;
  private final List<? extends SelectField<?>> allFields;

  @Inject
  public TagRepo(
      final AgroalDataSource dataSource,
      @ConfigProperty(name = "filearch.encryption-key", defaultValue = "LOCAL_DEV_ENCRYPTION_KEY")
          final String encryptionKey) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();

    this.encryptionKey = encryptionKey;

    this.allFields =
        List.of(
            Tags.TAGS.ID,
            Tags.TAGS.OWNER_USER_ID,
            decryptField(Tags.TAGS.TAG_NAME, encryptionKey).as(DECRYPTED_TAG_NAME));
  }

  public Uni<PaginatedData<Tag>> getAll(
      final long userId, final Long after, final Integer limit, final SortDirection sortDirection) {
    SelectConditionStep<?> selectStatement =
        context.select(allFields).from(Tags.TAGS).where(Tags.TAGS.OWNER_USER_ID.eq(userId));

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(selectStatement, Tags.TAGS.ID, after, limit, sortDirection);

    List<Tag> data = finalQuery.fetch().map(this::toTagModel);

    return Uni.createFrom().item(RepoUtils.toPaginatedData(after, data, sortDirection, limit));
  }

  public Uni<PaginatedData<Tag>> getAllTagsForFile(
      final long userId,
      final Long fileId,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    SelectConditionStep<?> selectStatement =
        context
            .select(allFields)
            .from(FileTags.FILE_TAGS)
            .join(Tags.TAGS)
            .on(FileTags.FILE_TAGS.TAG_ID.eq(Tags.TAGS.ID))
            .where(Tags.TAGS.OWNER_USER_ID.eq(userId))
            .and(FileTags.FILE_TAGS.FILE_ID.eq(fileId));

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(selectStatement, Tags.TAGS.ID, after, limit, sortDirection);

    return Uni.createFrom()
        .item(
            RepoUtils.toPaginatedData(
                after, finalQuery.fetch().map(this::toTagModel), sortDirection, limit));
  }

  public Uni<Tag> createTag(final long userId, final String tagName) {
    Map<String, Object> insertFields =
        Map.of(
            Tags.TAGS.OWNER_USER_ID.getName(),
            userId,
            Tags.TAGS.TAG_NAME.getName(),
            encryptField(tagName, encryptionKey));

    return Uni.createFrom()
        .item(
            context
                .insertInto(Tags.TAGS)
                .set(insertFields)
                .onConflictDoNothing()
                .returningResult(allFields)
                .fetchOne())
        .map(this::toTagModel);
  }

  public Uni<Boolean> tagNameExists(final long userId, final String tagName) {
    return Uni.createFrom()
        .item(
            context
                .select(DSL.count().as("tags_with_name"))
                .from(Tags.TAGS)
                .where(Tags.TAGS.OWNER_USER_ID.eq(userId))
                .and(decryptField(Tags.TAGS.TAG_NAME, encryptionKey).eq(tagName))
                .fetchOne())
        .map(dbRecord -> dbRecord != null && dbRecord.getValue(0, Integer.class) > 0);
  }

  public Uni<Tag> getTagById(final long tagId, final long userId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(Tags.TAGS)
                .where(Tags.TAGS.ID.eq(tagId).and(Tags.TAGS.OWNER_USER_ID.eq(userId)))
                .fetchOne())
        .map(this::toTagModel);
  }

  public Uni<Tag> updateName(final long tagId, final long userId, final String newTagName) {
    Map<String, Object> encryptedNameMap =
        Map.of(Tags.TAGS.TAG_NAME.getName(), encryptField(newTagName, encryptionKey));

    return Uni.createFrom()
        .item(
            context
                .update(Tags.TAGS)
                .set(encryptedNameMap)
                .where(Tags.TAGS.ID.eq(tagId).and(Tags.TAGS.OWNER_USER_ID.eq(userId)))
                .returningResult(allFields)
                .fetchOne())
        .map(this::toTagModel);
  }

  public Uni<Boolean> deleteTag(final long userId, final long tagId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(Tags.TAGS)
                .where(Tags.TAGS.OWNER_USER_ID.eq(userId))
                .and(Tags.TAGS.ID.eq(tagId))
                .execute())
        .map(tagDeleted -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting tag {}", tagId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<List<Tag>> searchTags(
      final long userId, final String searchText, final Integer limit) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(Tags.TAGS)
                .where(Tags.TAGS.OWNER_USER_ID.eq(userId))
                .and(
                    decryptField(Tags.TAGS.TAG_NAME, encryptionKey)
                        .likeIgnoreCase("%" + searchText + "%"))
                .limit(limit == null ? DEFAULT_SEARCH_LIMIT : limit)
                .fetch())
        .map(resultList -> resultList.stream().map(this::toTagModel).toList())
        .onFailure()
        .invoke(ex -> log.error("Error searching tags {}", searchText, ex))
        .onFailure()
        .recoverWithItem(List.of());
  }

  Tag toTagModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return Tag.builder()
        .id(toConvert.get(Tags.TAGS.ID))
        .ownerId(toConvert.get(Tags.TAGS.OWNER_USER_ID))
        .tagName(toConvert.get(DECRYPTED_TAG_NAME, String.class))
        .build();
  }
}
