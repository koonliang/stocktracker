package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class DashboardResourceTest extends IntegrationTestSupport {
  @Test
  void returnsEmptyDashboardWhenThereAreNoTransactions() {
    var response =
        given()
            .when()
            .get("/api/dashboard")
            .then()
            .statusCode(200)
            .extract()
            .as(DashboardResponse.class);

    assertTrue(response.holdings().isEmpty());
    assertEquals(0.0, response.summary().totalMarketValue(), 0.0001);
    assertEquals(0.0, response.summary().totalCostBasis(), 0.0001);
  }

  @Test
  void returnsHoldingsForPersistedTransactions() throws Exception {
    persistTransaction("2024-03-01", "NVDA", "buy", "5", "100.0000", "0.0000");

    var response =
        given()
            .when()
            .get("/api/dashboard")
            .then()
            .statusCode(200)
            .extract()
            .as(DashboardResponse.class);

    assertEquals(1, response.holdings().size());
    assertEquals("NVDA", response.holdings().getFirst().ticker());
    assertEquals(5.0, response.holdings().getFirst().shares(), 0.0001);
  }
}
