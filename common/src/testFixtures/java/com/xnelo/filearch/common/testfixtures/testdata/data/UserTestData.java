package com.xnelo.filearch.common.testfixtures.testdata.data;

import com.xnelo.filearch.jooq.tables.Users;
import com.xnelo.filearch.jooq.tables.records.UsersRecord;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.jooq.DSLContext;

public class UserTestData {
  public static final Set<UsersRecord> users =
      Set.of(
          new UsersRecord(
              1L,
              "alice1".getBytes(StandardCharsets.UTF_8),
              "Alice".getBytes(StandardCharsets.UTF_8),
              "Cooper".getBytes(StandardCharsets.UTF_8),
              "ac@garbage.com".getBytes(StandardCharsets.UTF_8),
              "64bd3322-49e8-4ef5-971c-ded5df158ff6",
              1L),
          new UsersRecord(
              2L,
              "CC".getBytes(StandardCharsets.UTF_8),
              "Claire".getBytes(StandardCharsets.UTF_8),
              "Cooper".getBytes(StandardCharsets.UTF_8),
              "cc@garbage.com".getBytes(StandardCharsets.UTF_8),
              "50eebbc5-96e2-4e6d-8bd8-35c76778743e",
              2L));

  public static void clearDatabaseTable(DSLContext context) {
    context.truncate(Users.USERS).execute();
  }

  public static void loadTestData(DSLContext context) {
    context.batchInsert(users).execute();
  }
}
