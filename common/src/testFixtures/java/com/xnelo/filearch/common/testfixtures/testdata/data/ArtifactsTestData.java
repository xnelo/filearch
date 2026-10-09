package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.Artifacts;
import com.xnelo.filearch.jooq.tables.records.ArtifactsRecord;
import java.util.Set;
import org.jooq.DSLContext;

public class ArtifactsTestData {
  public static final Set<ArtifactsRecord> artifacts =
      Set.of(new ArtifactsRecord(1L, 1L, 1L, "1/1", "image/jpeg"));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(Artifacts.ARTIFACTS).execute();
  }

  public static void loadTestData(DSLContext context) {
    // Ensure the data is "dirty" so it
    // will be inserted correctly
    artifacts.forEach(a -> a.touched(true));

    context.batchInsert(artifacts).execute();
  }
}
