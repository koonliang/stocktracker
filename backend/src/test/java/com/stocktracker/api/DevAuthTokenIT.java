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
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The dev token endpoint returns the latest usable dev token in dev mode (FR-T02) and is absent
 * (404) in cognito mode.
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class DevAuthTokenIT extends IntegrationTestSupport {
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
        .contentType(ContentType.JSON)
        .body(Map.of("email", EMAIL))
        .when()
        .post("/api/auth/forgot-password")
        .then()
        .statusCode(202);

    given()
        .queryParam("email", EMAIL)
        .queryParam("purpose", "PASSWORD_RESET")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("purpose", equalTo("PASSWORD_RESET"));
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
