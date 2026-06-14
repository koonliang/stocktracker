package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.dto.QuoteResponse;
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
class QuotesResourceTest extends IntegrationTestSupport {

  private QuoteResponse.QuoteView quote(QuoteResponse response, String symbol) {
    return response.quotes().stream()
        .filter(q -> q.symbol().equalsIgnoreCase(symbol))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void returnsCachedQuoteForKnownSymbol() {
    // First read of a known instrument triggers an on-demand fetch (FR-006 cold-scheduler nuance).
    var response =
        given()
            .when()
            .get("/api/quotes?symbols=NVDA")
            .then()
            .statusCode(200)
            .extract()
            .as(QuoteResponse.class);

    var nvda = quote(response, "NVDA");
    assertNotNull(nvda.price());
    assertTrue(nvda.price() > 0);
    assertTrue(!nvda.stale(), "a freshly fetched quote should not be stale");
  }

  @Test
  void unknownSymbolReturnsNullPriceAndStale() {
    var response =
        given()
            .when()
            .get("/api/quotes?symbols=ZZZZ")
            .then()
            .statusCode(200)
            .extract()
            .as(QuoteResponse.class);

    var unknown = quote(response, "ZZZZ");
    assertNull(unknown.price());
    assertTrue(unknown.stale());
  }

  @Test
  void blankSymbolsReturnsEmptyList() {
    var response =
        given()
            .when()
            .get("/api/quotes?symbols=")
            .then()
            .statusCode(200)
            .extract()
            .as(QuoteResponse.class);

    assertTrue(response.quotes().isEmpty());
  }
}
