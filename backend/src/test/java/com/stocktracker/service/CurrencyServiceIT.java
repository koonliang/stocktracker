package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.FxRate;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class CurrencyServiceIT extends IntegrationTestSupport {
  @Inject CurrencyService currencyService;

  private static final LocalDate TODAY = LocalDate.now();

  @BeforeEach
  void clearRates() throws Exception {
    inTransaction(() -> FxRate.deleteAll());
  }

  private void persistRate(String base, String quote, LocalDate date, String rate)
      throws Exception {
    inTransaction(
        () -> {
          var row = new FxRate();
          row.baseCurrency = base;
          row.quoteCurrency = quote;
          row.rateDate = date;
          row.rate = new BigDecimal(rate);
          row.source = "test";
          row.stale = false;
          row.persist();
        });
  }

  @Test
  void sameCurrencyConvertsUnchangedAndNotStale() {
    var result = currencyService.convert(new BigDecimal("100"), "USD", "USD", TODAY);
    assertEquals(0, result.value().compareTo(new BigDecimal("100")));
    assertFalse(result.stale());
    assertEquals(TODAY, result.fxDate());
    assertEquals(FxStatus.current, result.fxStatus());
  }

  @Test
  void convertsNativeToBaseUsingDailyRate() throws Exception {
    persistRate("USD", "SGD", TODAY, "1.35");

    var result = currencyService.convert(new BigDecimal("100"), "USD", "SGD", TODAY);

    assertEquals(0, result.value().compareTo(new BigDecimal("135.0000")));
    assertFalse(result.stale());
    assertEquals(TODAY, result.fxDate());
    assertEquals(FxStatus.current, result.fxStatus());
  }

  @Test
  void mixedCurrencyTotalReconcilesToNativeTimesFx() throws Exception {
    persistRate("USD", "SGD", TODAY, "1.35");

    // 1000 USD + 1000 SGD, reported in SGD: 1000*1.35 + 1000 = 2350 SGD.
    var usdLeg = currencyService.convert(new BigDecimal("1000"), "USD", "SGD", TODAY);
    var sgdLeg = currencyService.convert(new BigDecimal("1000"), "SGD", "SGD", TODAY);
    var total = usdLeg.value().add(sgdLeg.value());

    assertEquals(0, total.compareTo(new BigDecimal("2350.0000")));
  }

  @Test
  void valuationDateFallsBackToLastKnownRateMarkedStale() throws Exception {
    // Only yesterday's rate exists; converting for today must use it and keep it current.
    persistRate("USD", "SGD", TODAY.minusDays(1), "1.35");

    var result = currencyService.convertHolding(new BigDecimal("100"), "USD", "SGD", TODAY);

    assertEquals(0, result.value().compareTo(new BigDecimal("135.0000")));
    assertFalse(result.stale());
    assertEquals(TODAY.minusDays(1), result.fxDate());
    assertEquals(FxStatus.current, result.fxStatus());
  }

  @Test
  void valuationDateOlderThanOneDayFallsBackMarkedStale() throws Exception {
    persistRate("USD", "SGD", TODAY.minusDays(2), "1.35");

    var result = currencyService.convertHolding(new BigDecimal("100"), "USD", "SGD", TODAY);

    assertEquals(0, result.value().compareTo(new BigDecimal("135.0000")));
    assertTrue(result.stale());
    assertEquals(TODAY.minusDays(2), result.fxDate());
    assertEquals(FxStatus.stale, result.fxStatus());
  }

  @Test
  void transactionDateUsesTransactionDayRate() throws Exception {
    var transactionDate = TODAY.minusDays(10);
    persistRate("USD", "SGD", transactionDate, "1.40");
    persistRate("USD", "SGD", TODAY, "1.35");

    var result =
        currencyService.convertTransaction(new BigDecimal("100"), "USD", "SGD", transactionDate);

    assertEquals(0, result.value().compareTo(new BigDecimal("140.0000")));
    assertEquals(transactionDate, result.fxDate());
    assertEquals(FxStatus.current, result.fxStatus());
  }

  @Test
  void exactInverseRateBeatsOlderDirectFallback() throws Exception {
    persistRate("USD", "SGD", TODAY.minusDays(10), "1.40");
    persistRate("SGD", "USD", TODAY, "0.740741");

    var result = currencyService.convertTransaction(new BigDecimal("100"), "USD", "SGD", TODAY);

    assertEquals(TODAY, result.fxDate());
    assertEquals(FxStatus.current, result.fxStatus());
  }

  @Test
  void absentPairReturnsUnavailableWithoutPassThroughAmount() {
    var result = currencyService.convert(new BigDecimal("100"), "USD", "EUR", TODAY);

    assertEquals(0, result.value().compareTo(BigDecimal.ZERO));
    assertEquals(FxStatus.unavailable, result.fxStatus());
    assertTrue(result.unavailable());
  }
}
