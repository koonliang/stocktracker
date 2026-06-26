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
 * In enhanced dev mode, sign-up creates an active verified account immediately; duplicate sign-up
 * stays non-enumerating; password policy still applies.
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class SignUpVerifyIT extends IntegrationTestSupport {
  private static final String EMAIL = "newuser@example.com";
  private static final String PASSWORD = "Passw0rd!";

  @BeforeEach
  void cleanAccounts() throws Exception {
    inTransaction(
        () -> {
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
        .body("status", equalTo("account_created"));
  }

  @Test
  void signupCreatesVerifiedActiveAccount() {
    signup(EMAIL);
    var user = AppUser.<AppUser>find("email", EMAIL).firstResult();
    org.junit.jupiter.api.Assertions.assertEquals(AppUser.Status.ACTIVE, user.status);
    org.junit.jupiter.api.Assertions.assertTrue(user.emailVerified);
    org.junit.jupiter.api.Assertions.assertEquals(0, VerificationToken.count("userId", user.id));
  }

  @Test
  void duplicateSignupIsNonEnumerating() {
    signup(EMAIL);
    // A second sign-up with the same email returns the identical accepted response.
    signup(EMAIL);
    org.junit.jupiter.api.Assertions.assertEquals(1, AppUser.count("email", EMAIL));
  }

  @Test
  void newlyCreatedAccountCanLoginImmediately() {
    signup(EMAIL);
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
