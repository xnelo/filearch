package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.GroupMemberPermission;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.jooq.tables.GroupMemberPermissions;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@RequestScoped
@Slf4j
public class GroupMemberPermissionsRepo {
  private final DSLContext context;

  @Inject
  public GroupMemberPermissionsRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();
  }

  public Uni<List<GroupMemberPermission>> getPermissions(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .where(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID.eq(userId))
                .and(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID.eq(groupId))
                .fetch()
                .map(this::toGroupMemberPermission));
  }

  public Uni<Boolean> deleteUserPermissionsFromGroup(final long userId, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .where(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID.eq(groupId))
                .and(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID.eq(userId))
                .execute())
        .map(numDeleted -> numDeleted > 0)
        .onFailure()
        .invoke(ex -> log.error("Error deleting user permissions", ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> deleteAllGroupPermissions(final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .where(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID.eq(groupId))
                .execute())
        .map(numDeleted -> numDeleted > 0)
        .onFailure()
        .invoke(ex -> log.error("Error deleting all group permissions", ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<GroupMemberPermission> addPermission(
      final long userId, final long groupId, final GroupPermissionType permission) {
    return Uni.createFrom()
        .item(
            context
                .insertInto(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .set(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID, userId)
                .set(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID, groupId)
                .set(
                    GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.PERMISSION_GRANTED,
                    permission.getDbValue())
                .returningResult(DSL.asterisk())
                .fetchOne())
        .map(this::toGroupMemberPermission);
  }

  public Uni<Boolean> removePermission(
      final long userId, final long groupId, final GroupPermissionType permission) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .where(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID.eq(userId))
                .and(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID.eq(groupId))
                .and(
                    GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.PERMISSION_GRANTED.eq(
                        permission.getDbValue()))
                .execute())
        .map(numDeleted -> numDeleted > 0)
        .onFailure()
        .invoke(
            ex ->
                log.error(
                    "Error deleting permission userId={} groupId={} permission={}",
                    userId,
                    groupId,
                    permission,
                    ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<Boolean> permissionExists(
      final long userId, final long groupId, final GroupPermissionType permission) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS)
                .where(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID.eq(userId))
                .and(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID.eq(groupId))
                .and(
                    GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.PERMISSION_GRANTED.eq(
                        permission.getDbValue()))
                .fetchOne())
        .map(Objects::nonNull)
        .onFailure()
        .invoke(
            ex ->
                log.error(
                    "Error looking up group permissions for userid={} groupid={} permission={}",
                    userId,
                    groupId,
                    permission,
                    ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  GroupMemberPermission toGroupMemberPermission(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return GroupMemberPermission.builder()
        .userId(toConvert.get(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.USER_ID))
        .groupId(toConvert.get(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.GROUP_ID))
        .permission(
            GroupPermissionType.fromDbValue(
                toConvert.get(GroupMemberPermissions.GROUP_MEMBER_PERMISSIONS.PERMISSION_GRANTED)))
        .build();
  }
}
