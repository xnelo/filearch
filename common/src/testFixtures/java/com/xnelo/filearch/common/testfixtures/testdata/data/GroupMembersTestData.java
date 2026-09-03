package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.GroupMembers;
import com.xnelo.filearch.jooq.tables.records.GroupMembersRecord;
import java.util.Set;
import org.jooq.DSLContext;

public class GroupMembersTestData {
  public static final Set<GroupMembersRecord> groupMembers =
      Set.of(new GroupMembersRecord(1L, 1L, true));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(GroupMembers.GROUP_MEMBERS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(groupMembers).execute();
  }
}
