package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.dto.InstrumentAnalysisResponse;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class InstrumentResourceIT extends IntegrationTestSupport {
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

  @Test
  void derivesSnapshotStatsWhenStaticStatsAreMissing() throws Exception {
    inTransaction(
        () -> {
          InstrumentPriceBar.delete("instrumentSymbol", "NSTT");
          Instrument.delete("symbol", "NSTT");

          var instrument = new Instrument();
          instrument.symbol = "NSTT";
          instrument.name = "No Stat Test";
          instrument.sector = "Technology";
          instrument.exchange = "NASDAQ";
          instrument.currency = "USD";
          instrument.active = true;
          instrument.persist();

          persistBar("NSTT", "2026-01-02", "99.00", "105.00", "95.00", "102.00", 1200L);
          persistBar("NSTT", "2026-01-03", "101.00", "110.00", "100.00", "108.00", 1500L);
        });

    var response =
        given()
            .when()
            .get("/api/instruments/NSTT")
            .then()
            .statusCode(200)
            .extract()
            .as(InstrumentAnalysisResponse.class);

    assertNotNull(response.stats());
    assertEquals(101.0, response.stats().open(), 0.0001);
    assertEquals(102.0, response.stats().previousClose(), 0.0001);
    assertEquals(110.0, response.stats().week52High(), 0.0001);
    assertEquals(95.0, response.stats().week52Low(), 0.0001);
    assertEquals(1500L, response.stats().volume());
  }

  @Test
  void fiveYearRangeBackfillsHistoryForDynamicInstrument() throws Exception {
    inTransaction(
        () -> {
          InstrumentPriceBar.delete("instrumentSymbol", "DYN5");
          Instrument.delete("symbol", "DYN5");

          var instrument = new Instrument();
          instrument.symbol = "DYN5";
          instrument.name = "Dynamic Five Year";
          instrument.sector = "Unknown";
          instrument.exchange = "TEST";
          instrument.currency = "USD";
          instrument.active = true;
          instrument.persist();
        });

    var response =
        given()
            .queryParam("range", "5Y")
            .when()
            .get("/api/instruments/DYN5")
            .then()
            .statusCode(200)
            .extract()
            .as(InstrumentAnalysisResponse.class);

    assertEquals("DYN5", response.ticker().symbol());
    assertEquals(true, response.priceHistory().size() > 1000);
    assertEquals(
        true,
        LocalDate.parse(response.priceHistory().get(0).date())
            .isBefore(LocalDate.now().minusYears(4).minusMonths(11)));
  }

  @Test
  void allRangeBackfillsMaxHistoryForShortDynamicInstrument() throws Exception {
    inTransaction(
        () -> {
          InstrumentPriceBar.delete("instrumentSymbol", "MAXT");
          Instrument.delete("symbol", "MAXT");

          var instrument = new Instrument();
          instrument.symbol = "MAXT";
          instrument.name = "Max History Test";
          instrument.sector = "Unknown";
          instrument.exchange = "TEST";
          instrument.currency = "USD";
          instrument.active = true;
          instrument.persist();
        });

    var response =
        given()
            .queryParam("range", "ALL")
            .when()
            .get("/api/instruments/MAXT")
            .then()
            .statusCode(200)
            .extract()
            .as(InstrumentAnalysisResponse.class);

    assertEquals("MAXT", response.ticker().symbol());
    assertEquals(true, response.priceHistory().size() > 2000);
    assertEquals(
        true,
        LocalDate.parse(response.priceHistory().get(0).date())
            .isBefore(LocalDate.now().minusYears(9).minusMonths(11)));
  }

  private void persistBar(
      String symbol, String date, String open, String high, String low, String close, long volume) {
    var bar = new InstrumentPriceBar();
    bar.instrumentSymbol = symbol;
    bar.tradeDate = LocalDate.parse(date);
    bar.openPrice = new BigDecimal(open);
    bar.highPrice = new BigDecimal(high);
    bar.lowPrice = new BigDecimal(low);
    bar.closePrice = new BigDecimal(close);
    bar.volume = volume;
    bar.persist();
  }
}
