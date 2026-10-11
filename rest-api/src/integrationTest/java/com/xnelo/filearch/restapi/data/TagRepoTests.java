package com.xnelo.filearch.restapi.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xnelo.filearch.common.testfixtures.testdata.DatabaseTestData;
import com.xnelo.filearch.common.testfixtures.testresource.PostgresTestResource;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
public class TagRepoTests {
  private static AgroalDataSource dataSource;

  private TagRepo tagRepo;

  @BeforeAll
  static void setupClass() {
    dataSource = Arc.container().instance(AgroalDataSource.class).get();
    DatabaseTestData testData = new DatabaseTestData(dataSource, "FILEARCH");
    testData.cleanDatabase();
    testData.loadDataIntoDatabase();
  }

  @BeforeEach
  void setupTest() {
    // Encryption is disabled for tests
    tagRepo = new TagRepo(dataSource, "");
  }

  @Nested
  class TagVisibleToUserMethodTests {
    @Test
    void tagInDatabaseAndUserIsOwner() {
      boolean res = tagRepo.tagVisibleToUser(1L, 1L).await().indefinitely();
      assertTrue(res);
    }

    @Test
    void tagNotInDatabase() {
      boolean res = tagRepo.tagVisibleToUser(1L, 10000000L).await().indefinitely();
      assertFalse(res);
    }

    @Test
    void tagSharedWithUser() {
      boolean res = tagRepo.tagVisibleToUser(2L, 1L).await().indefinitely();
      assertTrue(res);
    }

    @Test
    void tagExistsButNotSharedWithUser() {
      boolean res = tagRepo.tagVisibleToUser(2L, 2L).await().indefinitely();
      assertFalse(res);
    }
  }
}
