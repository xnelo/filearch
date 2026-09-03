package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.Folders;
import com.xnelo.filearch.jooq.tables.records.FoldersRecord;
import java.util.Set;
import org.jooq.DSLContext;

public class FoldersTestData {
  public static final Set<FoldersRecord> folders =
      Set.of(new FoldersRecord(1L, 1L, null, null), new FoldersRecord(2L, 2L, null, null));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(Folders.FOLDERS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(folders).execute();
  }
}
