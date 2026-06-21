package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class AuthSocialExchangeTest extends IntegrationTestSupport {
  @BeforeEach
  void resetSocialAccounts() throws Exception {
    inTransaction(
        () -> {
          SocialIdentity.deleteAll();
          AppUser.delete("email like ?1", "%@social.local");
        });
  }

  @Test
  void exchangesGoogleCodeIntoLocalSession() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("code", "social-google-user", "redirectUri", "http://localhost:5173/auth/callback"))
        .when()
        .post("/api/auth/social/google/exchange")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("user.email", equalTo("social-google-user@social.local"));
  }

  @Test
  void rejectsMissingSocialCode() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("code", "", "redirectUri", "http://localhost:5173/auth/callback"))
        .when()
        .post("/api/auth/social/facebook/exchange")
        .then()
        .statusCode(400)
        .body("code", equalTo("AUTH_FAILED"));
  }
}

