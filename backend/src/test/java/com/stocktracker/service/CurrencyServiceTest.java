package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.FxRate;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.persistence.FxRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CurrencyServiceTest {
  private final FxRateRepository fxRates = Mockito.mock(FxRateRepository.class);
  private CurrencyService service;

  @BeforeEach
  void setUp() {
    service = new CurrencyService();
    service.fxRates = fxRates;
  }

  @Test
  void returnsExactDirectRateAsCurrent() {
    var date = LocalDate.of(2025, 1, 2);
    when(fxRates.find("USD", "SGD", date))
        .thenReturn(Optional.of(rate("USD", "SGD", date, "1.35", false)));

    var converted = service.convert(new BigDecimal("10"), "USD", "SGD", date);

    assertEquals(new BigDecimal("13.5000"), converted.value());
    assertEquals(FxStatus.current, converted.fxStatus());
  }

  @Test
  void usesInverseFallbackAndMarksStale() {
    var requestedDate = LocalDate.of(2025, 2, 10);
    when(fxRates.find("JPY", "USD", requestedDate)).thenReturn(Optional.empty());
    when(fxRates.find("USD", "JPY", requestedDate)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("JPY", "USD", requestedDate)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("USD", "JPY", requestedDate))
        .thenReturn(Optional.of(rate("USD", "JPY", requestedDate.minusDays(2), "150.0", false)));

    var converted = service.rate("JPY", "USD", requestedDate);

    assertTrue(converted.isPresent());
    assertEquals(FxStatus.stale, converted.get().fxStatus());
    assertEquals(0, converted.get().value().compareTo(new BigDecimal("0.00666667")));
  }

  @Test
  void crossConvertsThroughUsdPivot() {
    var date = LocalDate.of(2025, 3, 3);
    when(fxRates.find("EUR", "SGD", date)).thenReturn(Optional.empty());
    when(fxRates.find("SGD", "EUR", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("EUR", "SGD", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("SGD", "EUR", date)).thenReturn(Optional.empty());
    when(fxRates.find("EUR", "USD", date))
        .thenReturn(Optional.of(rate("EUR", "USD", date, "1.10", false)));
    when(fxRates.find("USD", "SGD", date))
        .thenReturn(Optional.of(rate("USD", "SGD", date, "1.35", false)));

    var converted = service.rate("EUR", "SGD", date);

    assertTrue(converted.isPresent());
    assertEquals(FxStatus.current, converted.get().fxStatus());
    assertEquals(0, converted.get().value().compareTo(new BigDecimal("1.48500000")));
  }

  @Test
  void returnsUnavailableWhenNoRateExists() {
    var date = LocalDate.of(2025, 4, 1);
    when(fxRates.find("CHF", "JPY", date)).thenReturn(Optional.empty());
    when(fxRates.find("JPY", "CHF", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("CHF", "JPY", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("JPY", "CHF", date)).thenReturn(Optional.empty());
    when(fxRates.find("CHF", "USD", date)).thenReturn(Optional.empty());
    when(fxRates.find("USD", "CHF", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("CHF", "USD", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("USD", "CHF", date)).thenReturn(Optional.empty());
    when(fxRates.find("USD", "JPY", date)).thenReturn(Optional.empty());
    when(fxRates.find("JPY", "USD", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("USD", "JPY", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("JPY", "USD", date)).thenReturn(Optional.empty());

    var converted = service.convert(new BigDecimal("5"), "CHF", "JPY", date);

    assertEquals(FxStatus.unavailable, converted.fxStatus());
    assertEquals(0, converted.value().compareTo(BigDecimal.ZERO));
  }

  @Test
  void convertHandlesNullAmountAndSameCurrency() {
    var date = LocalDate.of(2025, 5, 1);

    var nullAmount = service.convert(null, "USD", "SGD", date);
    var sameCurrency = service.convert(new BigDecimal("5"), "usd", "USD", date);

    assertEquals(new BigDecimal("0"), nullAmount.value());
    assertEquals(FxStatus.current, nullAmount.fxStatus());
    assertEquals(new BigDecimal("5"), sameCurrency.value());
  }

  @Test
  void usesInverseExactAndHonorsExplicitStaleFlag() {
    var date = LocalDate.of(2025, 2, 10);
    when(fxRates.find("SGD", "USD", date)).thenReturn(Optional.empty());
    when(fxRates.find("USD", "SGD", date))
        .thenReturn(Optional.of(rate("USD", "SGD", date, "1.35", true)));

    var converted = service.rate("SGD", "USD", date);

    assertTrue(converted.isPresent());
    assertEquals(FxStatus.stale, converted.get().fxStatus());
  }

  @Test
  void usesDirectFallbackAsCurrentWhenRecent() {
    var date = LocalDate.of(2025, 2, 10);
    when(fxRates.find("USD", "SGD", date)).thenReturn(Optional.empty());
    when(fxRates.find("SGD", "USD", date)).thenReturn(Optional.empty());
    when(fxRates.findLatestOnOrBefore("USD", "SGD", date))
        .thenReturn(Optional.of(rate("USD", "SGD", date.minusDays(1), "1.34", false)));

    var converted = service.rate("USD", "SGD", date);

    assertTrue(converted.isPresent());
    assertEquals(FxStatus.current, converted.get().fxStatus());
  }

  private FxRate rate(String base, String quote, LocalDate date, String value, boolean stale) {
    var row = new FxRate();
    row.baseCurrency = base;
    row.quoteCurrency = quote;
    row.rateDate = date;
    row.rate = new BigDecimal(value);
    row.stale = stale;
    return row;
  }
}
