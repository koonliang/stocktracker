package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.FxRate;
import com.stocktracker.dto.TransactionRequest;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class TransactionValidationServiceIT extends IntegrationTestSupport {
  @Inject TransactionValidationService transactionValidationService;

  private static final LocalDate TODAY = LocalDate.now();

  @Test
  void cashDepositWithFxBackedCurrencyIsValid() throws Exception {
    var request =
        new TransactionRequest(
            TODAY.minusYears(1), null, "deposit", null, null, null, new BigDecimal("1000"), "SGD");
    var issue = transactionValidationService.validate(request, java.util.Map.of());
    assertEquals(null, issue);
    assertTrue(
        FxRate.count(
                "baseCurrency = ?1 and quoteCurrency = ?2 and rateDate = ?3",
                "USD",
                "SGD",
                TODAY.minusYears(1))
            > 0);
  }

  @Test
  void cashDepositFailsWhenFxToBaseCurrencyIsUnavailable() throws Exception {
    persistFxRate("EUR", "JPY", TODAY.toString(), "170.00");

    var request =
        new TransactionRequest(
            TODAY, null, "deposit", null, null, null, new BigDecimal("1000"), "JPY");
    var issue = transactionValidationService.validate(request, java.util.Map.of());
    assertEquals("FX rate unavailable for JPY to USD", issue);
  }

  @Test
  void cashDepositAllowsStaleFxFallback() throws Exception {
    persistFxRate("USD", "JPY", TODAY.minusDays(2).toString(), "155.20");

    var request =
        new TransactionRequest(
            TODAY, null, "deposit", null, null, null, new BigDecimal("1000"), "JPY");
    var issue = transactionValidationService.validate(request, java.util.Map.of());
    assertEquals(null, issue);
  }

  @Test
  void securityBuyAllowsInstrumentCurrencyOutsideLegacyAllowlist() throws Exception {
    persistInstrument("SGD7203.SI", "SGD Test Instrument", "SGX", "SGD");

    var request =
        new TransactionRequest(
            TODAY,
            "SGD7203.SI",
            "buy",
            new BigDecimal("10"),
            new BigDecimal("100"),
            BigDecimal.ZERO,
            null,
            "SGD");
    var issue = transactionValidationService.validate(request, java.util.Map.of());
    assertEquals(null, issue);
    assertTrue(
        FxRate.count(
                "baseCurrency = ?1 and quoteCurrency = ?2 and rateDate = ?3", "USD", "SGD", TODAY)
            > 0);
  }

  @Test
  void securityBuyFailsWhenInstrumentCurrencyCannotConvertToBase() throws Exception {
    persistInstrument("JPY7204.T", "Toyota Motor Corp", "TSE", "JPY");
    persistFxRate("EUR", "JPY", TODAY.toString(), "170.00");

    var request =
        new TransactionRequest(
            TODAY,
            "JPY7204.T",
            "buy",
            new BigDecimal("10"),
            new BigDecimal("100"),
            BigDecimal.ZERO,
            null,
            "JPY");
    var issue = transactionValidationService.validate(request, java.util.Map.of());
    assertEquals("FX rate unavailable for JPY to USD", issue);
  }
}
