package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Login: success issues a JWT; failures are generic; unverified accounts get 403 (FR-002/012). */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class AuthLoginTest extends IntegrationTestSupport {
  private static final String PASSWORD = "Passw0rd!";

  @BeforeEach
  void seedAccounts() throws Exception {
    inTransaction(
        () -> {
          AuthCredential.deleteAll();
          AppUser.delete("email like ?1", "%@example.com");
          createUser("active@example.com", AppUser.Status.ACTIVE, true);
          createUser("unverified@example.com", AppUser.Status.UNVERIFIED, false);
        });
  }

  private void createUser(String email, AppUser.Status status, boolean verified) {
    var user = new AppUser();
    user.email = AppUser.normalizeEmail(email);
    user.status = status;
    user.emailVerified = verified;
    user.persist();
    var credential = new AuthCredential();
    credential.userId = user.id;
    credential.passwordHash = BcryptUtil.bcryptHash(PASSWORD);
    credential.persist();
  }

  @Test
  void issuesTokenForValidCredentials() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "active@example.com", "password", PASSWORD))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("user.email", equalTo("active@example.com"));
  }

  @Test
  void rejectsWrongPasswordWithGenericError() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "active@example.com", "password", "wrong-password"))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(401)
        .body("code", equalTo("AUTH_FAILED"));
  }

  @Test
  void rejectsUnknownEmailWithSameGenericError() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "ghost@example.com", "password", PASSWORD))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(401)
        .body("code", equalTo("AUTH_FAILED"));
  }

  @Test
  void blocksUnverifiedAccount() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "unverified@example.com", "password", PASSWORD))
        .when()
        .post("/api/auth/login")
        .then()
        .statusCode(403)
        .body("code", equalTo("EMAIL_UNVERIFIED"));
  }
}
