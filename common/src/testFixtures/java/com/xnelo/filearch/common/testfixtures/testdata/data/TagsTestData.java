package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.Tags;
import com.xnelo.filearch.jooq.tables.records.TagsRecord;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.jooq.DSLContext;

public class TagsTestData {
  public static final Set<TagsRecord> tags =
      Set.of(
          new TagsRecord(1L, 1L, "DOOD".getBytes(StandardCharsets.UTF_8)),
          new TagsRecord(2L, 1L, "DOOD2".getBytes(StandardCharsets.UTF_8)));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(Tags.TAGS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(tags).execute();
  }
}
