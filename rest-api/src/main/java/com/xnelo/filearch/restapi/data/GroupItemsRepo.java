package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.GroupItem;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.jooq.tables.GroupItems;
import com.xnelo.filearch.jooq.tables.records.GroupItemsRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@Slf4j
@RequestScoped
public class GroupItemsRepo {
  private final DSLContext context;

  public GroupItemsRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();
  }

  public Uni<Boolean> isItemInGroup(
      final long itemId, final GroupItemType groupItemType, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(GroupItems.GROUP_ITEMS)
                .where(GroupItems.GROUP_ITEMS.ITEM_ID.eq(itemId))
                .and(GroupItems.GROUP_ITEMS.ITEM_TYPE.eq(groupItemType.getDbValue()))
                .and(GroupItems.GROUP_ITEMS.GROUP_ID.eq(groupId))
                .fetchOne())
        .map(Objects::nonNull)
        .onFailure()
        .invoke(ex -> log.error("Error checking if item is in group.", ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }

  public Uni<GroupItem> addItemToGroup(
      final long itemId, final GroupItemType groupItemType, final long groupId) {
    GroupItemsRecord newRecord = new GroupItemsRecord(itemId, groupItemType.getDbValue(), groupId);

    return Uni.createFrom()
        .item(
            context
                .insertInto(GroupItems.GROUP_ITEMS)
                .set(newRecord)
                .onConflictDoNothing()
                .returningResult()
                .fetchOne())
        .map(this::toGroupItemModel);
  }

  public Uni<GroupItem> removeItemFromGroup(
      final long itemId, final GroupItemType groupItemType, final long groupId) {
    return Uni.createFrom()
        .item(
            context
                .deleteFrom(GroupItems.GROUP_ITEMS)
                .where(GroupItems.GROUP_ITEMS.GROUP_ID.eq(groupId))
                .and(GroupItems.GROUP_ITEMS.ITEM_ID.eq(itemId))
                .and(GroupItems.GROUP_ITEMS.ITEM_TYPE.eq(groupItemType.getDbValue()))
                .returningResult()
                .fetchOne())
        .map(this::toGroupItemModel)
        .onFailure()
        .invoke(ex -> log.error("Error deleting group item.", ex))
        .onFailure()
        .recoverWithItem((GroupItem) null);
  }

  GroupItem toGroupItemModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return GroupItem.builder()
        .itemId(toConvert.get(GroupItems.GROUP_ITEMS.ITEM_ID))
        .itemType(GroupItemType.fromDbValue(toConvert.get(GroupItems.GROUP_ITEMS.ITEM_TYPE)))
        .groupId(toConvert.get(GroupItems.GROUP_ITEMS.GROUP_ID))
        .build();
  }
}
