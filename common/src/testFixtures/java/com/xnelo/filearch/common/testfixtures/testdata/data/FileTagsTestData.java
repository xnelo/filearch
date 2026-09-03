package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.FileTags;
import com.xnelo.filearch.jooq.tables.records.FileTagsRecord;
import java.util.Set;
import org.jooq.DSLContext;

public class FileTagsTestData {
  public static final Set<FileTagsRecord> fileTags =
      Set.of(new FileTagsRecord(2L, 1L, 1L), new FileTagsRecord(1L, 1L, null));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(FileTags.FILE_TAGS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(fileTags).execute();
  }
}
