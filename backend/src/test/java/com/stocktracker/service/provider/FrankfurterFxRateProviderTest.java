package com.stocktracker.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FrankfurterFxRateProviderTest {
  private final FrankfurterApi api = Mockito.mock(FrankfurterApi.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private FrankfurterFxRateProvider provider;

  @BeforeEach
  void setUp() {
    provider = new FrankfurterFxRateProvider();
    provider.api = api;
  }

  @Test
  void dailyRatesUsesLatestForTodayAndSkipsBaseCurrency() throws Exception {
    var today = LocalDate.now();
    when(api.latest("USD", "SGD"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"date":"%s","rates":{"SGD":1.35}}
                """
                    .formatted(today)));

    var rates = provider.dailyRates("USD", List.of("SGD", "USD"), today);

    verify(api).latest("USD", "SGD");
    assertEquals(1, rates.size());
    assertEquals("SGD", rates.getFirst().quote());
  }

  @Test
  void dailyRatesUsesSpecificDateForHistoricalRequest() throws Exception {
    when(api.onDate("2025-01-07", "USD", "SGD"))
        .thenReturn(objectMapper.readTree("{\"date\":\"2025-01-07\",\"rates\":{\"SGD\":1.34}}"));

    var rates = provider.dailyRates("USD", List.of("SGD"), LocalDate.of(2025, 1, 7));

    verify(api).onDate("2025-01-07", "USD", "SGD");
    assertEquals(1.34, rates.getFirst().rate().doubleValue());
  }

  @Test
  void rangeRatesParsesV1RangeResponse() throws Exception {
    when(api.rangeV1("2025-01-06", "2025-01-07", "USD", "SGD,EUR"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"rates":{
                  "2025-01-06":{"SGD":1.34,"EUR":0.92},
                  "2025-01-07":{"SGD":1.35}
                }}
                """));

    var rates =
        provider.rangeRates(
            "USD", List.of("SGD", "EUR"), LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 7));

    assertEquals(3, rates.size());
    assertEquals(LocalDate.of(2025, 1, 6), rates.getFirst().date());
  }

  @Test
  void rangeRatesFallsBackToDailyLoopWhenRangeResponseIsEmpty() throws Exception {
    when(api.rangeV1("2025-01-06", "2025-01-07", "USD", "SGD"))
        .thenReturn(objectMapper.readTree("{\"rates\":{}}"));
    when(api.onDate("2025-01-06", "USD", "SGD"))
        .thenReturn(objectMapper.readTree("{\"date\":\"2025-01-06\",\"rates\":{\"SGD\":1.34}}"));
    when(api.onDate("2025-01-07", "USD", "SGD"))
        .thenReturn(objectMapper.readTree("{\"date\":\"2025-01-07\",\"rates\":{\"SGD\":1.35}}"));

    var rates =
        provider.rangeRates(
            "USD", List.of("SGD"), LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 7));

    assertEquals(2, rates.size());
    verify(api).onDate("2025-01-06", "USD", "SGD");
    verify(api).onDate("2025-01-07", "USD", "SGD");
  }

  @Test
  void rangeRatesReturnsEmptyForInvalidBounds() {
    assertTrue(
        provider
            .rangeRates("USD", List.of("SGD"), LocalDate.of(2025, 1, 7), LocalDate.of(2025, 1, 6))
            .isEmpty());
  }
}
