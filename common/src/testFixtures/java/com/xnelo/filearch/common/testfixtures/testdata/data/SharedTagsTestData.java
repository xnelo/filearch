package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.SharedTags;
import com.xnelo.filearch.jooq.tables.records.SharedTagsRecord;
import java.util.Set;
import org.jooq.DSLContext;

public class SharedTagsTestData {
  public static final Set<SharedTagsRecord> sharedTags = Set.of(new SharedTagsRecord(1L, 1L));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(SharedTags.SHARED_TAGS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(sharedTags).execute();
  }
}
