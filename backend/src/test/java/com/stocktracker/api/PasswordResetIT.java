package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.domain.VerificationToken;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Forgot-password is non-enumerating; reset updates the password, invalidates prior sessions
 * (FR-018), and the reset token is single-use (FR-016/017, SC-005/007).
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class PasswordResetIT extends IntegrationTestSupport {
  private static final String EMAIL = "reset@example.com";
  private static final String OLD_PASSWORD = "OldPass123!";
  private static final String NEW_PASSWORD = "NewPass456!";

  @BeforeEach
  void seedAccount() throws Exception {
    inTransaction(
        () -> {
          VerificationToken.delete(
              "userId in (select u.id from AppUser u where u.email like ?1)", "%@example.com");
          AuthCredential.delete(
              "userId in (select u.id from AppUser u where u.email like ?1)", "%@example.com");
          AppUser.delete("email like ?1", "%@example.com");
          var user = new AppUser();
          user.email = AppUser.normalizeEmail(EMAIL);
          user.status = AppUser.Status.ACTIVE;
          user.emailVerified = true;
          user.persist();
          var credential = new AuthCredential();
          credential.userId = user.id;
          credential.passwordHash = BcryptUtil.bcryptHash(OLD_PASSWORD);
          credential.persist();
        });
  }

  private void forgotPassword(String email) {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", email))
        .when()
        .post("/api/auth/forgot-password")
        .then()
        .statusCode(202)
        .body("status", equalTo("reset_sent"));
  }

  private String latestResetToken(String email) {
    return given()
        .queryParam("email", email)
        .queryParam("purpose", "PASSWORD_RESET")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(200)
        .extract()
        .path("token");
  }

  private String loginToken(String password) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", EMAIL, "password", password))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .path("token");
  }

  private int loginStatus(String password) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", EMAIL, "password", password))
        .when()
        .post("/api/auth/login")
        .then()
        .extract()
        .statusCode();
  }

  @Test
  void forgotPasswordIsNonEnumeratingForUnknownEmail() {
    // Unknown address returns the identical accepted response and issues no token.
    forgotPassword("ghost@example.com");
    given()
        .queryParam("email", "ghost@example.com")
        .queryParam("purpose", "PASSWORD_RESET")
        .when()
        .get("/api/dev/auth/latest-token")
        .then()
        .statusCode(404);
  }

  @Test
  void resetUpdatesPasswordAndRejectsOldOne() {
    forgotPassword(EMAIL);
    var token = latestResetToken(EMAIL);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("token", token, "newPassword", NEW_PASSWORD))
        .when()
        .post("/api/auth/reset-password")
        .then()
        .statusCode(200)
        .body("status", equalTo("reset"));

    var newToken = loginToken(NEW_PASSWORD);
    given()
        .header("Authorization", "Bearer " + newToken)
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(200)
        .body("email", equalTo(EMAIL));
    Assertions.assertEquals(401, loginStatus(OLD_PASSWORD));
  }

  @Test
  void resetInvalidatesExistingSessions() {
    var oldToken =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", EMAIL, "password", OLD_PASSWORD))
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .path("token");

    forgotPassword(EMAIL);
    var resetToken = latestResetToken(EMAIL);
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("token", resetToken, "newPassword", NEW_PASSWORD))
        .when()
        .post("/api/auth/reset-password")
        .then()
        .statusCode(200);

    // The pre-reset session token is no longer valid (FR-018).
    given()
        .header("Authorization", "Bearer " + oldToken)
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(401);
  }

  @Test
  void usedResetTokenIsRejectedOnReuse() {
    forgotPassword(EMAIL);
    var token = latestResetToken(EMAIL);
    var body = Map.of("token", token, "newPassword", NEW_PASSWORD);

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/auth/reset-password")
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/auth/reset-password")
        .then()
        .statusCode(400)
        .body("code", equalTo("TOKEN_INVALID"));
  }

  @Test
  void rejectsWeakNewPassword() {
    forgotPassword(EMAIL);
    var token = latestResetToken(EMAIL);
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("token", token, "newPassword", "short"))
        .when()
        .post("/api/auth/reset-password")
        .then()
        .statusCode(400)
        .body("code", equalTo("VALIDATION"));
  }
}
