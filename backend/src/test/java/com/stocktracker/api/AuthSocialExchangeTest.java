package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.stocktracker.client.FacebookAuthClient;
import com.stocktracker.client.GoogleAuthClient;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
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
    QuarkusMock.installMockForType(
        new GoogleAuthClient() {
          @Override
          public ProviderProfile exchange(String code, String redirectUri) {
            if ("social-google-user".equals(code)) {
              return new ProviderProfile(
                  "google-sub-social-user", "social-google-user@gmail.com", true);
            }
            throw new ApiException(
                jakarta.ws.rs.core.Response.Status.UNAUTHORIZED,
                "AUTH_FAILED",
                "Unable to complete sign-in.");
          }
        },
        GoogleAuthClient.class);
    QuarkusMock.installMockForType(
        new FacebookAuthClient() {
          @Override
          public GoogleAuthClient.ProviderProfile exchange(String code, String redirectUri) {
            if ("social-facebook-user".equals(code)) {
              return new GoogleAuthClient.ProviderProfile(
                  "facebook-sub-social-user", "social-facebook-user@example.com", true);
            }
            throw new ApiException(
                jakarta.ws.rs.core.Response.Status.UNAUTHORIZED,
                "AUTH_FAILED",
                "Unable to complete sign-in.");
          }
        },
        FacebookAuthClient.class);

    inTransaction(
        () -> {
          SocialIdentity.deleteAll();
          AppUser.delete("email like ?1 or email like ?2", "%@gmail.com", "%@example.com");
        });
  }

  @Test
  void exchangesGoogleCodeIntoLocalSession() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "code", "social-google-user", "redirectUri", "http://localhost:5173/auth/callback"))
        .when()
        .post("/api/auth/social/google/exchange")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("user.email", equalTo("social-google-user@gmail.com"));
  }

  @Test
  void exchangesFacebookCodeIntoLocalSession() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "code",
                "social-facebook-user",
                "redirectUri",
                "http://localhost:5173/auth/callback"))
        .when()
        .post("/api/auth/social/facebook/exchange")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("user.email", equalTo("social-facebook-user@example.com"));
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
