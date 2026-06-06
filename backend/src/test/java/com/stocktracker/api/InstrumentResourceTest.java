package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.stocktracker.dto.InstrumentAnalysisResponse;
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
class InstrumentResourceTest extends IntegrationTestSupport {
  @Test
  void returnsInstrumentAnalysisIncludingHeldPosition() throws Exception {
    persistTransaction("2024-04-15", "NVDA", "buy", "7", "125.0000", "0.0000");

    var response =
        given()
            .when()
            .get("/api/instruments/NVDA")
            .then()
            .statusCode(200)
            .extract()
            .as(InstrumentAnalysisResponse.class);

    assertEquals("NVDA", response.ticker().symbol());
    assertNotNull(response.stats());
    assertNotNull(response.positionSummary());
    assertEquals(7.0, response.positionSummary().shares(), 0.0001);
  }

  @Test
  void returnsNotFoundForUnknownTicker() {
    given().when().get("/api/instruments/ZZZZ").then().statusCode(404);
  }
}
