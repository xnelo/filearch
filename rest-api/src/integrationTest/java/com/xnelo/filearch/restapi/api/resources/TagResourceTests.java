package com.xnelo.filearch.restapi.api.resources;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xnelo.filearch.common.testfixtures.testdata.DatabaseTestData;
import com.xnelo.filearch.common.testfixtures.testresource.PostgresTestResource;
import com.xnelo.filearch.restapi.api.contracts.FileContract;
import com.xnelo.filearch.restapi.testing.TestJsonWebTokenProducer;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
public class TagResourceTests {
  @Inject TestJsonWebTokenProducer jwtProducer;

  @BeforeAll
  static void setupClass() {
    AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
    DatabaseTestData testData = new DatabaseTestData(dataSource, "FILEARCH");
    testData.cleanDatabase();
    testData.loadDataIntoDatabase();
  }

  @Nested
  class AssignedFilesEndpoint {
    @Test
    @TestSecurity(authorizationEnabled = false)
    void userDoesNotExist() {
      jwtProducer.setUserIdToUse("11111111-2222-3333-4444-555555555555");

      Response tagsResponse = given().when().get("/tag/1/assigned_files");

      assertEquals(404, tagsResponse.getStatusCode());

      JsonPath jsonPath = tagsResponse.jsonPath();
      String errorMsg =
          jsonPath.getObject("action_responses[0].errors[0].error_message", String.class);
      assertEquals("User does not exist", errorMsg);
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void tagDoesNotExist() {
      final long TAG_ID_TO_USE = 999999999999L;
      jwtProducer.setUserIdToUse("64bd3322-49e8-4ef5-971c-ded5df158ff6");

      Response tagsResponse = given().when().get("/tag/" + TAG_ID_TO_USE + "/assigned_files");

      assertEquals(404, tagsResponse.getStatusCode());

      JsonPath jsonPath = tagsResponse.jsonPath();
      String errorMsg =
          jsonPath.getObject("action_responses[0].errors[0].error_message", String.class);
      assertEquals("Tag (" + TAG_ID_TO_USE + ") does not exist.", errorMsg);
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void validTagOwnedByUser() {
      final long TAG_ID_TO_USE = 1L;
      jwtProducer.setUserIdToUse("64bd3322-49e8-4ef5-971c-ded5df158ff6");

      Response tagsResponse = given().when().get("/tag/" + TAG_ID_TO_USE + "/assigned_files");

      assertEquals(200, tagsResponse.getStatusCode());

      JsonPath jsonPath = tagsResponse.jsonPath();
      List<FileContract> files =
          jsonPath.getList("action_responses[0].data.data", FileContract.class);
      assertEquals(2, files.size());
      assertEquals(1L, files.getFirst().getId());
      assertEquals(2L, files.get(1).getId());
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void validTagNotOwnedByUser() {
      final long TAG_ID_TO_USE = 1L;
      jwtProducer.setUserIdToUse("50eebbc5-96e2-4e6d-8bd8-35c76778743e");

      Response tagsResponse = given().when().get("/tag/" + TAG_ID_TO_USE + "/assigned_files");

      assertEquals(200, tagsResponse.getStatusCode());

      JsonPath jsonPath = tagsResponse.jsonPath();
      List<FileContract> files =
          jsonPath.getList("action_responses[0].data.data", FileContract.class);
      assertEquals(1, files.size());
      assertEquals(2L, files.getFirst().getId());
    }
  }
}
