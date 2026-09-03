package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.GroupItems;
import com.xnelo.filearch.jooq.tables.records.GroupItemsRecord;
import java.util.Set;
import org.jooq.DSLContext;

public class GroupItemsTestData {
  public static final Set<GroupItemsRecord> groupItems =
      Set.of(new GroupItemsRecord(2L, (short) 2, 1L));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(GroupItems.GROUP_ITEMS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(groupItems).execute();
  }
}
