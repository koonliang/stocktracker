package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.domain.VerificationToken;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The dev token endpoint returns the latest token in dev mode (FR-T02) and is absent (404) in
 * cognito mode.
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class DevAuthTokenTest extends IntegrationTestSupport {
  private static final String EMAIL = "devtoken@example.com";

  @BeforeEach
  void cleanAccounts() throws Exception {
    inTransaction(
        () -> {
          VerificationToken.deleteAll();
          AuthCredential.delete(
              "userId in (select u.id from AppUser u where u.email like ?1)", "%@example.com");
          AppUser.delete("email like ?1", "%@example.com");
        });
  }

  @Test
  void returnsLatestTokenInDevMode() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", EMAIL, "password", "Passw0rd!"))
        .when()
        .post("/api/auth/signup")
        .then()
        .statusCode(202);

    given()
        .queryParam("email", EMAIL)
        .queryParam("purpose", "EMAIL_VERIFICATION")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("purpose", equalTo("EMAIL_VERIFICATION"));
  }

  @Test
  void returns404WhenNoTokenExists() {
    given()
        .queryParam("email", "nobody@example.com")
        .queryParam("purpose", "EMAIL_VERIFICATION")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(404);
  }
}

/** In cognito mode the dev token route is absent: every request returns 404. */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestProfile(DevAuthTokenCognitoModeTest.CognitoProfile.class)
class DevAuthTokenCognitoModeTest {
  public static class CognitoProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("stocktracker.auth.mode", "cognito");
    }
  }

  @Test
  void devTokenEndpointAbsentInCognitoMode() {
    given()
        .queryParam("email", "anyone@example.com")
        .queryParam("purpose", "EMAIL_VERIFICATION")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(404);
  }
}
