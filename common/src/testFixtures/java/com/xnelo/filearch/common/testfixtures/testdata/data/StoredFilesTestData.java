package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.StoredFiles;
import com.xnelo.filearch.jooq.tables.records.StoredFilesRecord;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.jooq.DSLContext;

public class StoredFilesTestData {
  public static final Set<StoredFilesRecord> storedFiles =
      Set.of(
          new StoredFilesRecord(
              1L, 1L, 1L, "LFS", "1/1", "test.png".getBytes(StandardCharsets.UTF_8), "image/png"),
          new StoredFilesRecord(
              2L, 1L, 1L, "LFS", "1/2", "test2.png".getBytes(StandardCharsets.UTF_8), "image/png"));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(StoredFiles.STORED_FILES).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(storedFiles).execute();
  }
}
