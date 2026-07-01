package com.stocktracker.service;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.domain.Watchlist;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Data isolation (FR-004/006): authenticated as the seed user, another user's watchlist and
 * transactions are invisible and unreachable — foreign ids return 404, never 403.
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class DataIsolationIT extends IntegrationTestSupport {
  private static final String OTHER_EMAIL = "userb-isolation@example.com";

  long foreignWatchlistId;
  long foreignTransactionId;

  @BeforeEach
  void seedForeignData() throws Exception {
    inTransaction(
        () -> {
          AppUser.delete("email = ?1", OTHER_EMAIL);
          var other = new AppUser();
          other.email = OTHER_EMAIL;
          other.status = AppUser.Status.ACTIVE;
          other.emailVerified = true;
          other.persist();

          var watchlist = new Watchlist();
          watchlist.userId = other.id;
          watchlist.name = "Their List";
          watchlist.persist();
          foreignWatchlistId = watchlist.id;

          var transaction = new PortfolioTransaction();
          transaction.userId = other.id;
          transaction.tradeDate = LocalDate.parse("2024-05-01");
          transaction.instrumentSymbol = "AAPL";
          transaction.transactionType = "buy";
          transaction.quantity = new BigDecimal("3");
          transaction.price = new BigDecimal("100.0000");
          transaction.fees = BigDecimal.ZERO;
          transaction.source = "MANUAL";
          transaction.persist();
          foreignTransactionId = transaction.id;
        });
  }

  @Test
  void anotherUsersWatchlistIsNotListed() {
    given()
        .when()
        .get("/api/watchlists")
        .then()
        .statusCode(200)
        .body("watchlists.id", not(Matchers.hasItem(String.valueOf(foreignWatchlistId))));
  }

  @Test
  void cannotRenameAnotherUsersWatchlist() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Hijacked"))
        .when()
        .patch("/api/watchlists/{id}", foreignWatchlistId)
        .then()
        .statusCode(404);
  }

  @Test
  void cannotAddTickerToAnotherUsersWatchlist() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("ticker", "AAPL"))
        .when()
        .post("/api/watchlists/{id}/tickers", foreignWatchlistId)
        .then()
        .statusCode(404);
  }

  @Test
  void cannotDeleteAnotherUsersTransaction() {
    given().when().delete("/api/transactions/{id}", foreignTransactionId).then().statusCode(404);
  }
}
