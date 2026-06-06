package com.stocktracker.api;

import static io.restassured.RestAssured.given;

import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Unauthenticated callers are rejected on data endpoints; instrument reference stays public. */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class AuthGuardTest {
  @Test
  void protectedEndpointsRequireAuthentication() {
    given().when().get("/api/dashboard").then().statusCode(401);
    given().when().get("/api/transactions").then().statusCode(401);
    given().when().get("/api/watchlists").then().statusCode(401);
  }

  @Test
  void instrumentReferenceEndpointStaysPublic() {
    given().when().get("/api/instruments/AAPL").then().statusCode(200);
  }
}
