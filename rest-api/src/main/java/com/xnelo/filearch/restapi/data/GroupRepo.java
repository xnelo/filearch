package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.Group;
import com.xnelo.filearch.common.model.GroupFile;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.model.GroupMember;
import com.xnelo.filearch.common.model.GroupMemberType;
import com.xnelo.filearch.common.model.GroupMembershipStatus;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.jooq.tables.Folders;
import com.xnelo.filearch.jooq.tables.GroupItems;
import com.xnelo.filearch.jooq.tables.GroupMemberPermissions;
import com.xnelo.filearch.jooq.tables.GroupMembers;
import com.xnelo.filearch.jooq.tables.Groups;
import com.xnelo.filearch.jooq.tables.StoredFiles;
import com.xnelo.filearch.jooq.tables.Users;
import com.xnelo.filearch.jooq.tables.records.GroupMembersRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.SelectField;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

@Slf4j
@RequestScoped
public class GroupRepo {
  public static final String DECRYPTED_GROUP_NAME = "DECRYPT_GROUP_NAME";
  public static final String GROUPS_IN_IS_ADMIN = "IS_ADMIN";
  public static final String GROUP_MEMBERSHIP_TYPE = "GROUP_MEMBERSHIP_TYPE";

  public static final String DECRYPTED_USERNAME = "decrypted_username_col";

  private static final String ADMIN_TABLE_NAME = "admin_table";
  private static final String CTE_USER_ID = "CTE_USER_ID";
  private static final String CTE_GROUP_ID = "CTE_GROUP_ID";

  private static final String DECRYPTED_FOLDER_NAME = "DECRYPTED_FOLDER_NAME";
  private static final String DECRYPTED_ORIGINAL_FILENAME = "DECRYPTED_ORIGINAL_FILENAME";

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

  public Uni<PaginatedData<Group>> getGroupsIn(
      final long userId,
      final GroupMembershipStatus membershipStatus,
      final PaginationParameters paginationParameters) {

    CommonTableExpression<?> adminTable = getAdminTable();

    SelectConditionStep<?> selectStatment =
        context
            .with(adminTable)
            .select(
                Groups.GROUPS.ID,
                Groups.GROUPS.OWNER_USER_ID,
                decryptField(Groups.GROUPS.GROUP_NAME, encryptionKey).as(DECRYPTED_GROUP_NAME),
                GroupMembers.GROUP_MEMBERS.USER_ID,
                GroupMembers.GROUP_MEMBERS.ACCEPTED,
                DSL.case_()
                    .when(Groups.GROUPS.OWNER_USER_ID.eq(userId), GroupMemberType.OWNER.name())
                    .when(
                        Objects.requireNonNull(adminTable.field(GROUPS_IN_IS_ADMIN)).isTrue(),
                        GroupMemberType.ADMIN.name())
                    .otherwise(GroupMemberType.MEMBER.name())
                    .as(GROUP_MEMBERSHIP_TYPE))
            .from(Groups.GROUPS)
            .join(GroupMembers.GROUP_MEMBERS)
            .on(Groups.GROUPS.ID.eq(GroupMembers.GROUP_MEMBERS.GROUP_ID))
            .leftOuterJoin(adminTable)
            .on(
                Objects.requireNonNull(adminTable.field(CTE_GROUP_ID, Long.class))
                    .eq(Groups.GROUPS.ID))
            .and(
                Objects.requireNonNull(adminTable.field(CTE_USER_ID, Long.class))
                    .eq(GroupMembers.GROUP_MEMBERS.USER_ID))
            .where(GroupMembers.GROUP_MEMBERS.USER_ID.eq(userId));

    // If membershipStatus is null or GroupMembershipStatus.ALL then do nothing to the query.
    if (membershipStatus != null) {
      if (membershipStatus == GroupMembershipStatus.JOINED) {
        selectStatment = selectStatment.and(GroupMembers.GROUP_MEMBERS.ACCEPTED.isTrue());
      } else if (membershipStatus == GroupMembershipStatus.PENDING) {
        selectStatment =
            selectStatment.and(
                GroupMembers.GROUP_MEMBERS
                    .ACCEPTED
                    .isNull()
                    .or(GroupMembers.GROUP_MEMBERS.ACCEPTED.isFalse()));
      }
    }

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(selectStatment, Groups.GROUPS.ID, paginationParameters);

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

  public Uni<Boolean> userInGroup(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(GroupMembers.GROUP_MEMBERS)
                .where(GroupMembers.GROUP_MEMBERS.GROUP_ID.eq(groupId))
                .and(GroupMembers.GROUP_MEMBERS.USER_ID.eq(userId))
                .fetchOne())
        .map(Objects::nonNull)
        .onFailure()
        .invoke(ex -> log.error("Error in user {} in group {}", userId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> userActiveMemberInGroup(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(GroupMembers.GROUP_MEMBERS)
                .where(GroupMembers.GROUP_MEMBERS.USER_ID.eq(userId))
                .and(GroupMembers.GROUP_MEMBERS.GROUP_ID.eq(groupId))
                .and(GroupMembers.GROUP_MEMBERS.ACCEPTED.isTrue())
                .fetchOne())
        .map(Objects::nonNull)
        .onFailure()
        .invoke(
            ex -> log.error("Error getting user {} membership in group {}", userId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> acceptGroupInvite(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .update(GroupMembers.GROUP_MEMBERS)
                .set(GroupMembers.GROUP_MEMBERS.ACCEPTED, true)
                .where(GroupMembers.GROUP_MEMBERS.GROUP_ID.eq(groupId))
                .and(GroupMembers.GROUP_MEMBERS.USER_ID.eq(userId))
                .execute())
        .map(updateResult -> (updateResult != null && updateResult == 1))
        .onFailure()
        .invoke(ex -> log.error("Error accepting group invite {}", groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> removeUserFromGroup(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupMembers.GROUP_MEMBERS)
                .where(GroupMembers.GROUP_MEMBERS.USER_ID.eq(userId))
                .and(GroupMembers.GROUP_MEMBERS.GROUP_ID.eq(groupId))
                .execute())
        .map(updateResult -> (updateResult != null && updateResult == 1))
        .onFailure()
        .invoke(ex -> log.error("Error removing user {} from group {}", userId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Long> getOwnerOfGroup(final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .select(Groups.GROUPS.OWNER_USER_ID)
                .from(Groups.GROUPS)
                .where(Groups.GROUPS.ID.eq(groupId))
                .fetchOne(Groups.GROUPS.OWNER_USER_ID))
        .onFailure()
        .invoke(ex -> log.error("Error getting owner of group {}", groupId, ex))
        .onFailure()
        .recoverWithItem((Long) null);
  }

  Group toGroupModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    Group.GroupBuilder builder =
        Group.builder()
            .id(toConvert.get(Groups.GROUPS.ID))
            .ownerId(toConvert.get(Groups.GROUPS.OWNER_USER_ID))
            .name(toConvert.get(DECRYPTED_GROUP_NAME, String.class));

    if (toConvert.field(GroupMembers.GROUP_MEMBERS.ACCEPTED) != null) {
      builder.accepted(toConvert.get(GroupMembers.GROUP_MEMBERS.ACCEPTED));
    } else {
      builder.accepted(true);
    }

    if (toConvert.field(GROUP_MEMBERSHIP_TYPE) != null) {
      String rawGroupMembershipType = toConvert.get(GROUP_MEMBERSHIP_TYPE, String.class);
      GroupMemberType membershipType = GroupMemberType.valueOf(rawGroupMembershipType);
      builder.groupMembershipType(membershipType);
    } else {
      builder.groupMembershipType(GroupMemberType.OWNER);
    }

    return builder.build();
  }

  GroupMember toGroupMember(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    GroupMember.GroupMemberBuilder builder =
        GroupMember.builder()
            .userId(toConvert.get(Users.USERS.ID))
            .username(toConvert.get(DECRYPTED_USERNAME, String.class))
            .groupId(toConvert.get(GroupMembers.GROUP_MEMBERS.GROUP_ID));

    if (toConvert.field(GroupMembers.GROUP_MEMBERS.ACCEPTED) != null) {
      builder.accepted(toConvert.get(GroupMembers.GROUP_MEMBERS.ACCEPTED));
    } else {
      builder.accepted(true);
    }

    if (toConvert.field(GROUP_MEMBERSHIP_TYPE) != null) {
      String rawGroupMembershipType = toConvert.get(GROUP_MEMBERSHIP_TYPE, String.class);
      GroupMemberType membershipType = GroupMemberType.valueOf(rawGroupMembershipType);
      builder.memberType(membershipType);
    } else {
      builder.memberType(GroupMemberType.OWNER);
    }

    return builder.build();
  }

  public Uni<List<GroupMember>> getUsersInGroup(final long groupId) {
    CommonTableExpression<?> adminTable = getAdminTable();

    SelectConditionStep<?> selectStatement =
        context
            .with(adminTable)
            .select(
                Users.USERS.ID,
                decryptField(Users.USERS.USERNAME, encryptionKey).as(DECRYPTED_USERNAME),
                GroupMembers.GROUP_MEMBERS.GROUP_ID,
                GroupMembers.GROUP_MEMBERS.ACCEPTED,
                DSL.case_()
                    .when(
                        Groups.GROUPS.OWNER_USER_ID.eq(Users.USERS.ID),
                        GroupMemberType.OWNER.name())
                    .when(
                        Objects.requireNonNull(adminTable.field(GROUPS_IN_IS_ADMIN)).isTrue(),
                        GroupMemberType.ADMIN.name())
                    .otherwise(GroupMemberType.MEMBER.name())
                    .as(GROUP_MEMBERSHIP_TYPE))
            .from(GroupMembers.GROUP_MEMBERS)
            .join(Users.USERS)
            .on(Users.USERS.ID.eq(GroupMembers.GROUP_MEMBERS.USER_ID))
            .join(Groups.GROUPS)
            .on(Groups.GROUPS.ID.eq(GroupMembers.GROUP_MEMBERS.GROUP_ID))
            .leftOuterJoin(adminTable)
            .on(
                Objects.requireNonNull(adminTable.field(CTE_GROUP_ID, Long.class))
                    .eq(Groups.GROUPS.ID))
            .and(
                Objects.requireNonNull(adminTable.field(CTE_USER_ID, Long.class))
                    .eq(GroupMembers.GROUP_MEMBERS.USER_ID))
            .where(GroupMembers.GROUP_MEMBERS.GROUP_ID.eq(groupId));

    return Uni.createFrom().item(() -> selectStatement.fetch().map(this::toGroupMember));
  }

  private CommonTableExpression<?> getAdminTable() {
    return DSL.name(ADMIN_TABLE_NAME)
        .as(
            context
                .select(
                    GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID.as(CTE_USER_ID),
                    GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID.as(CTE_GROUP_ID),
                    DSL.inline(true).as(GROUPS_IN_IS_ADMIN))
                .from(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .where(
                    GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.PERMISSION_GRANTED.eq(
                        GroupPermissionType.ADMIN.getDbValue())));
  }

  public Uni<Boolean> groupExists(final long groupId) {
    var query = context.selectFrom(Groups.GROUPS).where(Groups.GROUPS.ID.eq(groupId));

    return Uni.createFrom().item(query::fetchOne).map(Objects::nonNull);
  }

  public Uni<PaginatedData<GroupFile>> getFilesInGroup(
      final long groupId, final PaginationParameters paginationParameters) {
    final String GROUP_FILE_CTE = "group_file_cte";
    CommonTableExpression<?> cte =
        DSL.name(GROUP_FILE_CTE)
            .as(
                context
                    .select(
                        StoredFiles.STORED_FILES.ID,
                        StoredFiles.STORED_FILES.OWNER_USER_ID,
                        StoredFiles.STORED_FILES.FOLDER_ID,
                        StoredFiles.STORED_FILES.STORAGE_TYPE,
                        StoredFiles.STORED_FILES.STORAGE_KEY,
                        decryptField(StoredFiles.STORED_FILES.ORIGINAL_FILENAME, encryptionKey)
                            .as(DECRYPTED_ORIGINAL_FILENAME),
                        StoredFiles.STORED_FILES.MIME_TYPE,
                        GroupItems.GROUP_ITEMS.ITEM_TYPE,
                        DSL.inline(null, SQLDataType.VARCHAR).as(DECRYPTED_FOLDER_NAME))
                    .from(StoredFiles.STORED_FILES)
                    .join(GroupItems.GROUP_ITEMS)
                    .on(StoredFiles.STORED_FILES.ID.eq(GroupItems.GROUP_ITEMS.ITEM_ID))
                    .where(GroupItems.GROUP_ITEMS.GROUP_ID.eq(groupId))
                    .and(GroupItems.GROUP_ITEMS.ITEM_TYPE.eq(GroupItemType.FILE.getDbValue()))
                    .unionAll(
                        context
                            .select(
                                StoredFiles.STORED_FILES.ID,
                                StoredFiles.STORED_FILES.OWNER_USER_ID,
                                StoredFiles.STORED_FILES.FOLDER_ID,
                                StoredFiles.STORED_FILES.STORAGE_TYPE,
                                StoredFiles.STORED_FILES.STORAGE_KEY,
                                decryptField(
                                        StoredFiles.STORED_FILES.ORIGINAL_FILENAME, encryptionKey)
                                    .as(DECRYPTED_ORIGINAL_FILENAME),
                                StoredFiles.STORED_FILES.MIME_TYPE,
                                GroupItems.GROUP_ITEMS.ITEM_TYPE,
                                decryptField(Folders.FOLDERS.NAME, encryptionKey)
                                    .as(DECRYPTED_FOLDER_NAME))
                            .from(StoredFiles.STORED_FILES)
                            .join(GroupItems.GROUP_ITEMS)
                            .on(
                                StoredFiles.STORED_FILES.FOLDER_ID.eq(
                                    GroupItems.GROUP_ITEMS.ITEM_ID))
                            .join(Folders.FOLDERS)
                            .on(StoredFiles.STORED_FILES.FOLDER_ID.eq(Folders.FOLDERS.ID))
                            .where(GroupItems.GROUP_ITEMS.GROUP_ID.eq(groupId))
                            .and(
                                GroupItems.GROUP_ITEMS.ITEM_TYPE.eq(
                                    GroupItemType.FOLDER.getDbValue()))));

    Field<Long> cteStoredFilesIdField = cte.field(StoredFiles.STORED_FILES.ID);
    Field<?> cteDecryptedFolderNameField = cte.field(DECRYPTED_FOLDER_NAME);

    if (cteStoredFilesIdField == null || cteDecryptedFolderNameField == null) {
      return Uni.createFrom()
          .item(new PaginatedData<>(null, List.of(), paginationParameters.getDir(), false));
    }

    SelectJoinStep<?> selectStatement =
        context.with(cte).select(DSL.asterisk()).distinctOn(cteStoredFilesIdField).from(cte);

    SelectLimitPercentStep<?> finalQuery =
        RepoUtils.addPagination(
            selectStatement,
            cteStoredFilesIdField,
            paginationParameters,
            List.of(cteDecryptedFolderNameField.asc()));

    return Uni.createFrom()
        .item(
            () -> {
              List<GroupFile> data = finalQuery.fetch().map(this::toGroupFileModel);
              return RepoUtils.toPaginatedData(data, paginationParameters);
            });
  }

  GroupFile toGroupFileModel(Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    StorageType storageType = null;
    if (toConvert.field(StoredFiles.STORED_FILES.STORAGE_TYPE) != null) {
      storageType = StorageType.fromString(toConvert.get(StoredFiles.STORED_FILES.STORAGE_TYPE));
    }

    GroupItemType groupItemType = null;
    if (toConvert.field(GroupItems.GROUP_ITEMS.ITEM_TYPE) != null) {
      groupItemType = GroupItemType.fromDbValue(toConvert.get(GroupItems.GROUP_ITEMS.ITEM_TYPE));
    }

    return new GroupFile(
        toConvert.get(StoredFiles.STORED_FILES.ID),
        toConvert.get(StoredFiles.STORED_FILES.OWNER_USER_ID),
        toConvert.get(StoredFiles.STORED_FILES.FOLDER_ID),
        storageType,
        toConvert.get(StoredFiles.STORED_FILES.STORAGE_KEY),
        toConvert.get(DECRYPTED_ORIGINAL_FILENAME, String.class),
        toConvert.get(StoredFiles.STORED_FILES.MIME_TYPE),
        groupItemType,
        toConvert.get(DECRYPTED_FOLDER_NAME, String.class));
  }
}
