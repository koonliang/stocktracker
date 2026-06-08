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
 * Sign-up creates an unverified account with a verification token; duplicate sign-up is
 * non-enumerating; unverified login is blocked; verification activates; tokens are single-use
 * (FR-009/011/012/013/014, SC-007).
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class SignUpVerifyTest extends IntegrationTestSupport {
  private static final String EMAIL = "newuser@example.com";
  private static final String PASSWORD = "Passw0rd!";

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

  private void signup(String email) {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", email, "password", PASSWORD))
        .when()
        .post("/api/auth/signup")
        .then()
        .statusCode(202)
        .body("status", equalTo("verification_sent"));
  }

  private String latestVerificationToken(String email) {
    return given()
        .queryParam("email", email)
        .queryParam("purpose", "EMAIL_VERIFICATION")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(200)
        .extract()
        .path("token");
  }

  @Test
  void signupCreatesUnverifiedAccountWithToken() {
    signup(EMAIL);
    var user = AppUser.<AppUser>find("email", EMAIL).firstResult();
    org.junit.jupiter.api.Assertions.assertEquals(AppUser.Status.UNVERIFIED, user.status);
    org.junit.jupiter.api.Assertions.assertEquals(1, VerificationToken.count("userId", user.id));
  }

  @Test
  void duplicateSignupIsNonEnumerating() {
    signup(EMAIL);
    // A second sign-up with the same email returns the identical accepted response.
    signup(EMAIL);
    org.junit.jupiter.api.Assertions.assertEquals(1, AppUser.count("email", EMAIL));
  }

  @Test
  void unverifiedLoginIsBlocked() {
    signup(EMAIL);
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", EMAIL, "password", PASSWORD))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(403)
        .body("code", equalTo("EMAIL_UNVERIFIED"));
  }

  @Test
  void verificationActivatesAndEnablesLogin() {
    signup(EMAIL);
    var token = latestVerificationToken(EMAIL);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("token", token))
        .when()
        .post("/api/auth/verify-email")
        .then()
        .statusCode(200)
        .body("status", equalTo("verified"));

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", EMAIL, "password", PASSWORD))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue());
  }

  @Test
  void usedTokenIsRejectedOnReuse() {
    signup(EMAIL);
    var token = latestVerificationToken(EMAIL);
    var body = Map.of("token", token);

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/auth/verify-email")
        .then()
        .statusCode(200);

    // Single-use: replaying the consumed token fails.
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/auth/verify-email")
        .then()
        .statusCode(400)
        .body("code", equalTo("TOKEN_INVALID"));
  }

  @Test
  void rejectsWeakPasswordWithValidationError() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "weak@example.com", "password", "short"))
        .when()
        .post("/api/auth/signup")
        .then()
        .statusCode(400)
        .body("code", equalTo("VALIDATION"));
  }
}
