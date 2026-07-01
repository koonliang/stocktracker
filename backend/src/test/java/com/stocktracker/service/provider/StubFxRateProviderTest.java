package com.stocktracker.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StubFxRateProviderTest {
  private StubFxRateProvider provider;

  @BeforeEach
  void setUp() {
    provider = new StubFxRateProvider();
    provider.objectMapper = new ObjectMapper();
  }

  @Test
  void dailyRatesReturnsFixtureRatesAndIdentityIsImplicitlySkipped() {
    var rates = provider.dailyRates("USD", List.of("SGD", "USD"), LocalDate.of(2025, 1, 7));

    assertEquals(2, rates.size());
    assertEquals("USD", rates.getFirst().base());
    assertEquals("SGD", rates.getFirst().quote());
    assertTrue(
        rates.stream()
            .anyMatch(
                rate ->
                    rate.quote().equals("USD")
                        && rate.rate().compareTo(java.math.BigDecimal.ONE) == 0));
  }

  @Test
  void rangeRatesReturnsEmptyForInvalidBounds() {
    assertTrue(
        provider
            .rangeRates("USD", List.of("SGD"), LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 7))
            .isEmpty());
  }

  @Test
  void rangeRatesExpandsDayByDay() {
    var rates =
        provider.rangeRates(
            "USD", List.of("SGD"), LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 8));

    assertEquals(3, rates.size());
    assertFalse(rates.isEmpty());
    assertEquals(LocalDate.of(2025, 1, 6), rates.getFirst().date());
    assertEquals(LocalDate.of(2025, 1, 8), rates.getLast().date());
  }
}
