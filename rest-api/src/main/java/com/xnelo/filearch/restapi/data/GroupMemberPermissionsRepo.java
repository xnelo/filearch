package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.GroupMemberPermission;
import com.xnelo.filearch.common.model.GroupPermissionType;
import com.xnelo.filearch.jooq.tables.GroupMemberPermissions;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@RequestScoped
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
