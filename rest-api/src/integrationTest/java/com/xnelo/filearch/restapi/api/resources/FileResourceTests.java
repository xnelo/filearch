package com.xnelo.filearch.restapi.api.resources;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xnelo.filearch.common.testfixtures.testdata.DatabaseTestData;
import com.xnelo.filearch.common.testfixtures.testresource.PostgresTestResource;
import com.xnelo.filearch.restapi.api.contracts.FilearchApiErrorResponse;
import com.xnelo.filearch.restapi.api.contracts.TagContract;
import com.xnelo.filearch.restapi.testing.TestJsonWebTokenProducer;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
public class FileResourceTests {
  @Inject TestJsonWebTokenProducer jwtProducer;

  @BeforeAll
  static void setupClass() {
    AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
    DatabaseTestData testData = new DatabaseTestData(dataSource, "FILEARCH");
    testData.loadDataIntoDatabase();
  }

  @Nested
  class FileTagsEndpoint {
    @Test
    @TestSecurity(authorizationEnabled = false)
    void getFileTags() {
      jwtProducer.setUserIdToUse("64bd3322-49e8-4ef5-971c-ded5df158ff6");

      Response tagsResponse = given().when().get("/file/1/tags");

      JsonPath jsonPath = tagsResponse.jsonPath();

      assertEquals(200, tagsResponse.getStatusCode());
      TagContract tagData =
          jsonPath.getObject("action_responses[0].data.data[0]", TagContract.class);

      assertEquals(1L, tagData.getId());
      assertEquals(1L, tagData.getOwnerId());
      assertEquals("DOOD", tagData.getTagName());
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void getFileTagsGroup() {
      jwtProducer.setUserIdToUse("64bd3322-49e8-4ef5-971c-ded5df158ff6");

      Response tagsResponse = given().when().get("/file/2/tags?group_id=1");

      JsonPath jsonPath = tagsResponse.jsonPath();

      assertEquals(200, tagsResponse.getStatusCode());
      TagContract tagData =
          jsonPath.getObject("action_responses[0].data.data[0]", TagContract.class);

      assertEquals(1L, tagData.getId());
      assertEquals(1L, tagData.getOwnerId());
      assertEquals("DOOD", tagData.getTagName());
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void getFileTagsUserNotInGroup() {
      jwtProducer.setUserIdToUse("50eebbc5-96e2-4e6d-8bd8-35c76778743e");

      Response tagsResponse = given().when().get("/file/2/tags?group_id=1");

      JsonPath jsonPath = tagsResponse.jsonPath();

      assertEquals(400, tagsResponse.getStatusCode());

      FilearchApiErrorResponse errorData =
          jsonPath.getObject("action_responses[0].errors[0]", FilearchApiErrorResponse.class);

      assertEquals(507, errorData.getErrorCode());
      assertEquals("User is not an active member of group(1).", errorData.getErrorMessage());
      assertEquals(400, errorData.getHttpCode());
    }
  }
}
