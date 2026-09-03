package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.Groups;
import com.xnelo.filearch.jooq.tables.records.GroupsRecord;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.jooq.DSLContext;

public class GroupsTestData {
  public static final Set<GroupsRecord> groups =
      Set.of(new GroupsRecord(1L, 1L, "TSTGRP".getBytes(StandardCharsets.UTF_8)));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(Groups.GROUPS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(groups).execute();
  }
}
