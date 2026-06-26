package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.stocktracker.domain.AppUser;
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
class DemoUserAuthResourceIT extends IntegrationTestSupport {
  @BeforeEach
  void resetDemoUsers() throws Exception {
    inTransaction(() -> AppUser.delete("accountKind", AppUser.AccountKind.DEMO));
  }

  @Test
  void createsAndListsDemoUsers() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("label", "Demo User 1"))
        .when()
        .post("/api/auth/demo-users")
        .then()
        .statusCode(201)
        .body("token", notNullValue())
        .body("demoUser.slot", equalTo(1));

    given()
        .when()
        .get("/api/auth/demo-users")
        .then()
        .statusCode(200)
        .body("users", hasSize(1))
        .body("canCreate", equalTo(true));
  }

  @Test
  void logsIntoExistingDemoUserBySlot() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of())
        .when()
        .post("/api/auth/demo-users")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/auth/demo-users/1/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("demoUser.label", equalTo("Demo User 1"));
  }
}
