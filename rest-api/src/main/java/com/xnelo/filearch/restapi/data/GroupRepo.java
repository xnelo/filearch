package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.Group;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.jooq.tables.GroupItems;
import com.xnelo.filearch.jooq.tables.GroupMembers;
import com.xnelo.filearch.jooq.tables.Groups;
import com.xnelo.filearch.jooq.tables.records.GroupMembersRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.SelectField;
import org.jooq.SelectLimitPercentStep;
import org.jooq.impl.DSL;

@Slf4j
@RequestScoped
public class GroupRepo {
  public static final String DECRYPTED_GROUP_NAME = "DECRYPT_GROUP_NAME";

  private final DSLContext context;
  private final String encryptionKey;
  private final List<? extends SelectField<?>> allFields;

  @Inject
  public GroupRepo(
      final AgroalDataSource dataSource,
      @ConfigProperty(name = "filearch.encryption-key", defaultValue = "LOCAL_DEV_ENCRYPTION_KEY")
          final String encryptionKey) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();

    this.encryptionKey = encryptionKey;

    this.allFields =
        List.of(
            Groups.GROUPS.ID,
            Groups.GROUPS.OWNER_USER_ID,
            decryptField(Groups.GROUPS.GROUP_NAME, encryptionKey).as(DECRYPTED_GROUP_NAME));
  }

  public Uni<PaginatedData<Group>> getAll(
      final long userId, final PaginationParameters paginationParameters) {
    SelectConditionStep<?> selectStatement =
        context.select(allFields).from(Groups.GROUPS).where(Groups.GROUPS.OWNER_USER_ID.eq(userId));

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(selectStatement, Groups.GROUPS.ID, paginationParameters);

    return Uni.createFrom()
        .item(
            () -> {
              List<Group> data = finalQuery.fetch().map(this::toGroupModel);
              return RepoUtils.toPaginatedData(data, paginationParameters);
            });
  }

  public Uni<Boolean> groupNameExists(final long userId, final String groupName) {
    return Uni.createFrom()
        .item(
            context
                .select(DSL.count().as("number_with_name"))
                .from(Groups.GROUPS)
                .where(Groups.GROUPS.OWNER_USER_ID.eq(userId))
                .and(decryptField(Groups.GROUPS.GROUP_NAME, encryptionKey).eq(groupName))
                .fetchOne())
        .map(dbRecord -> dbRecord != null && dbRecord.getValue(0, Integer.class) > 0);
  }

  public Uni<Group> createGroup(final long userId, final String groupName) {
    Map<String, Object> insertFields =
        Map.of(
            Groups.GROUPS.OWNER_USER_ID.getName(),
            userId,
            Groups.GROUPS.GROUP_NAME.getName(),
            encryptField(groupName, encryptionKey));
    return Uni.createFrom()
        .item(
            context
                .insertInto(Groups.GROUPS)
                .set(insertFields)
                .onConflictDoNothing()
                .returningResult(allFields)
                .fetchOne())
        .map(this::toGroupModel);
  }

  public Uni<Group> getGroupById(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(Groups.GROUPS)
                .where(Groups.GROUPS.ID.eq(groupId))
                .and(Groups.GROUPS.OWNER_USER_ID.eq(userId))
                .fetchOne())
        .map(this::toGroupModel);
  }

  public Uni<Boolean> deleteGroup(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(Groups.GROUPS)
                .where(Groups.GROUPS.OWNER_USER_ID.eq(userId))
                .and(Groups.GROUPS.ID.eq(groupId))
                .execute())
        .map(tagDeleted -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting group {}", groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> addUserToGroup(
      final long userId, final long groupId, final boolean alreadyAccepted) {
    GroupMembersRecord toInsert = new GroupMembersRecord(userId, groupId, alreadyAccepted);
    return Uni.createFrom()
        .item(context.insertInto(GroupMembers.GROUP_MEMBERS).set(toInsert).execute())
        .map(res -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error adding user {} to group {}", userId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> deleteAllUsersFromGroup(final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupMembers.GROUP_MEMBERS)
                .where(GroupMembers.GROUP_MEMBERS.GROUP_ID.eq(groupId))
                .execute())
        .map(res -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting group members from group {}", groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> deleteAllItemsFromGroup(final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupItems.GROUP_ITEMS)
                .where(GroupItems.GROUP_ITEMS.GROUP_ID.eq(groupId))
                .execute())
        .map(res -> Boolean.TRUE)
        .onFailure()
        .invoke(ex -> log.error("Error deleting group items from group {}", groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  Group toGroupModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return Group.builder()
        .id(toConvert.get(Groups.GROUPS.ID))
        .ownerId(toConvert.get(Groups.GROUPS.OWNER_USER_ID))
        .name(toConvert.get(DECRYPTED_GROUP_NAME, String.class))
        .build();
  }
}
