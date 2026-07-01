package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.domain.AppUser;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.dto.PerformanceResponse;
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
class PerformanceResourceIT extends IntegrationTestSupport {
  private void setBaseCurrency(String currency) throws Exception {
    inTransaction(
        () -> {
          var user = AppUser.<AppUser>findById(SEED_USER_ID);
          user.baseCurrency = currency;
        });
  }

  @Test
  void emptyPortfolioReturnsFlatPerformance() {
    var response =
        given()
            .when()
            .get("/api/performance?window=1Y&method=fifo")
            .then()
            .statusCode(200)
            .extract()
            .as(PerformanceResponse.class);

    assertEquals(0, response.realizedPnL());
    assertEquals(0, response.closedLots().size());
    assertEquals(0, response.incomeEvents().size());
    assertEquals(2, response.returnSeries().size());
  }

  @Test
  void methodParameterChangesClosedLotResults() throws Exception {
    persistTransaction("2024-01-01", "AAPL", "buy", "10", "100", "0");
    persistTransaction("2024-02-01", "AAPL", "buy", "10", "120", "0");
    persistTransaction("2024-03-01", "AAPL", "sell", "10", "130", "0");

    var fifo =
        given()
            .when()
            .get("/api/performance?window=ALL&method=fifo")
            .then()
            .statusCode(200)
            .extract()
            .as(PerformanceResponse.class);
    var lifo =
        given()
            .when()
            .get("/api/performance?window=ALL&method=lifo")
            .then()
            .statusCode(200)
            .extract()
            .as(PerformanceResponse.class);

    assertEquals(300, fifo.realizedPnL(), 0.01);
    assertEquals(100, lifo.realizedPnL(), 0.01);
  }

  @Test
  void dividendIncomeContributesToRealizedPnL() throws Exception {
    persistTransaction("2024-01-01", "AAPL", "buy", "10", "100", "0");
    persistTransaction("2024-02-01", "AAPL", "sell", "10", "130", "0");
    persistTransaction("2024-03-01", "AAPL", "dividend", "0", "0", "1.25", "50", "USD");

    var response =
        given()
            .when()
            .get("/api/performance?window=ALL&method=fifo")
            .then()
            .statusCode(200)
            .extract()
            .as(PerformanceResponse.class);

    assertEquals(348.75, response.realizedPnL(), 0.01);
    assertEquals(1, response.closedLots().size());
    assertEquals(300, response.closedLots().getFirst().realizedPnLBase(), 0.01);
    assertEquals(1, response.incomeEvents().size());
    assertEquals(48.75, response.incomeEvents().getFirst().amountBase(), 0.01);
  }

  @Test
  void exposesTransactionDateConversionMetadataForRealizedRows() throws Exception {
    setBaseCurrency("SGD");
    persistFxRate("USD", "SGD", "2024-02-01", "1.40");
    persistTransaction("2024-01-01", "AAPL", "buy", "10", "100", "0", null, "USD");
    persistTransaction("2024-02-01", "AAPL", "sell", "10", "130", "0", null, "USD");
    persistTransaction("2024-03-01", "AAPL", "dividend", "0", "0", "0", "50", "USD");

    var response =
        given()
            .when()
            .get("/api/performance?window=ALL&method=fifo")
            .then()
            .statusCode(200)
            .extract()
            .as(PerformanceResponse.class);

    assertEquals("SGD", response.baseCurrency());
    assertEquals(420.0, response.closedLots().getFirst().realizedPnLBase(), 0.01);
    assertEquals(
        FxStatus.current, response.closedLots().getFirst().realizedPnlConversion().fxStatus());
    assertEquals(
        "2024-02-01", response.closedLots().getFirst().realizedPnlConversion().fxDate().toString());
    assertEquals(
        FxStatus.current, response.incomeEvents().getFirst().amountConversion().fxStatus());
    assertEquals(
        "2024-03-01", response.incomeEvents().getFirst().amountConversion().fxDate().toString());
  }
}
