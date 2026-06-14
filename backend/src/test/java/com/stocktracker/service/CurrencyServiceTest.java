package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.FxRate;
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
class CurrencyServiceTest extends IntegrationTestSupport {
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
  }

  @Test
  void convertsNativeToBaseUsingDailyRate() throws Exception {
    persistRate("USD", "SGD", TODAY, "1.35");

    var result = currencyService.convert(new BigDecimal("100"), "USD", "SGD", TODAY);

    assertEquals(0, result.value().compareTo(new BigDecimal("135.0000")));
    assertFalse(result.stale());
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
  void missingPairFallsBackToLastKnownRateMarkedStale() throws Exception {
    // Only yesterday's rate exists; converting for today must use it, flagged stale.
    persistRate("USD", "SGD", TODAY.minusDays(1), "1.35");

    var result = currencyService.convert(new BigDecimal("100"), "USD", "SGD", TODAY);

    assertEquals(0, result.value().compareTo(new BigDecimal("135.0000")));
    assertTrue(result.stale());
  }

  @Test
  void absentPairPassesAmountThroughFlaggedStale() {
    var result = currencyService.convert(new BigDecimal("100"), "USD", "EUR", TODAY);

    assertEquals(0, result.value().compareTo(new BigDecimal("100")));
    assertTrue(result.stale());
  }
}
