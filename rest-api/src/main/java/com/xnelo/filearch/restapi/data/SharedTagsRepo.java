package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.exception.RepoException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.jooq.tables.GroupMembers;
import com.xnelo.filearch.jooq.tables.SharedTags;
import com.xnelo.filearch.jooq.tables.records.SharedTagsRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@Slf4j
@RequestScoped
public class SharedTagsRepo {
  private final DSLContext context;

  @Inject
  public SharedTagsRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();
  }

  public Uni<Boolean> tagShareExists(final Long tagId, final Long groupId) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(SharedTags.SHARED_TAGS)
                .where(SharedTags.SHARED_TAGS.TAG_ID.eq(tagId))
                .and(SharedTags.SHARED_TAGS.GROUP_ID.eq(groupId))
                .fetchOne())
        .map(Objects::nonNull)
        .onFailure()
        .invoke(
            ex -> log.error("Error checking if tag exists tagid:{} groupId:{}", tagId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> addSharedTag(final Long tagId, final Long groupId) {
    SharedTagsRecord sharedTag = new SharedTagsRecord(tagId, groupId);
    return Uni.createFrom()
        .item(context.insertInto(SharedTags.SHARED_TAGS).set(sharedTag).execute())
        .map(res -> res == 1)
        .onFailure()
        .invoke(ex -> log.error("Error adding shared tagid:{} groupId:{}", tagId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> deleteSharedTag(final Long tagId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(SharedTags.SHARED_TAGS)
                .where(SharedTags.SHARED_TAGS.TAG_ID.eq(tagId))
                .execute())
        .map(res -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting shared tagid:{}", tagId, ex))
        .onFailure()
        .transform(
            ex -> new RepoException(ErrorCode.DB_ERROR, "Error deleting shared tagid:" + tagId));
  }

  public Uni<Boolean> unshareTag(final Long tagId, final Long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(SharedTags.SHARED_TAGS)
                .where(SharedTags.SHARED_TAGS.TAG_ID.eq(tagId))
                .and(SharedTags.SHARED_TAGS.GROUP_ID.eq(groupId))
                .execute())
        .map(res -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error unshare tagid:{}", tagId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<List<Long>> groupsTagIsIn(final long userId, final long tagId) {
    return Uni.createFrom()
        .item(
            context
                .select(SharedTags.SHARED_TAGS.GROUP_ID)
                .from(SharedTags.SHARED_TAGS)
                .join(GroupMembers.GROUP_MEMBERS)
                .on(SharedTags.SHARED_TAGS.GROUP_ID.eq(GroupMembers.GROUP_MEMBERS.GROUP_ID))
                .where(GroupMembers.GROUP_MEMBERS.USER_ID.eq(userId))
                .and(GroupMembers.GROUP_MEMBERS.ACCEPTED.isTrue())
                .and(SharedTags.SHARED_TAGS.TAG_ID.eq(tagId))
                .fetchInto(Long.class))
        .onFailure()
        .invoke(ex -> log.error("Error getting groups tags are in. tagId:{}", tagId, ex))
        .onFailure()
        .transform(
            ex -> new RepoException(ErrorCode.DB_ERROR, "Error getting groups tags are in."));
  }
}
