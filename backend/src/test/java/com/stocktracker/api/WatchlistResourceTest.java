package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.stocktracker.dto.WatchlistResponse;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class WatchlistResourceTest extends IntegrationTestSupport {
  @Test
  void createsAndReordersWatchlistTickers() {
    var created =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Core Holdings"))
            .when()
            .post("/api/watchlists")
            .then()
            .statusCode(200)
            .extract()
            .as(WatchlistResponse.WatchlistItemView.class);

    var withNvda =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("ticker", "NVDA"))
            .when()
            .post("/api/watchlists/{watchlistId}/tickers", created.id())
            .then()
            .statusCode(200)
            .extract()
            .as(WatchlistResponse.WatchlistItemView.class);
    assertEquals(List.of("NVDA"), withNvda.tickers());
    assertEquals("NVIDIA Corporation", withNvda.instruments().getFirst().name());

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("ticker", "AAPL"))
        .when()
        .post("/api/watchlists/{watchlistId}/tickers", created.id())
        .then()
        .statusCode(200);

    var reordered =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("tickers", List.of("AAPL", "NVDA")))
            .when()
            .put("/api/watchlists/{watchlistId}/ticker-order", created.id())
            .then()
            .statusCode(200)
            .extract()
            .as(WatchlistResponse.WatchlistItemView.class);

    assertIterableEquals(List.of("AAPL", "NVDA"), reordered.tickers());
    assertIterableEquals(
        List.of("Apple Inc.", "NVIDIA Corporation"),
        reordered.instruments().stream()
            .map(WatchlistResponse.WatchlistInstrumentView::name)
            .toList());
  }

  @Test
  void rejectsUnknownTickerAdds() {
    var created =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Spec Watchlist"))
            .when()
            .post("/api/watchlists")
            .then()
            .statusCode(200)
            .extract()
            .as(WatchlistResponse.WatchlistItemView.class);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("ticker", "ZZZZ"))
        .when()
        .post("/api/watchlists/{watchlistId}/tickers", created.id())
        .then()
        .statusCode(422);
  }
}
