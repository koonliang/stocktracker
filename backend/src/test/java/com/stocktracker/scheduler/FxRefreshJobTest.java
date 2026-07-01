package com.stocktracker.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.FxRate;
import com.stocktracker.persistence.FxRateRepository;
import com.stocktracker.service.provider.FxRateProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FxRefreshJobTest {
  private final FxRateProvider fxRateProvider = Mockito.mock(FxRateProvider.class);
  private final FxRateRepository fxRates = Mockito.mock(FxRateRepository.class);
  private final EntityManager entityManager = Mockito.mock(EntityManager.class);
  private final TypedQuery<String> instrumentCurrencies = Mockito.mock(TypedQuery.class);
  private final TypedQuery<String> userCurrencies = Mockito.mock(TypedQuery.class);

  private FxRefreshJob job;

  @BeforeEach
  void setUp() {
    job = newSpyJob();
  }

  @Test
  void onStartRefreshesOnlyWhenEnabled() {
    Mockito.doNothing().when(job).refresh();
    job.onStart(null);
    verify(job).refresh();

    job = newSpyJob();
    job.enabled = false;
    Mockito.doNothing().when(job).refresh();

    job.onStart(null);

    verify(job, never()).refresh();
  }

  @Test
  void scheduledAlwaysRefreshes() {
    Mockito.doNothing().when(job).refresh();
    job.scheduled();

    verify(job).refresh();
  }

  @Test
  void refreshReturnsWhenOnlyOneCurrencyIsInUse() {
    mockCurrencies(List.of(), List.of());

    job.refresh();

    verify(fxRateProvider, never()).dailyRates(any(), any(), any());
  }

  @Test
  void refreshPersistsProviderRatesForEachBaseCurrency() {
    mockCurrencies(List.of("SGD"), List.of("EUR"));
    var usdSgd = new FxRate();
    var sgdUsd = new FxRate();
    var eurUsd = new FxRate();
    when(fxRates.findOrNew("USD", "SGD", LocalDate.parse("2026-06-26"))).thenReturn(usdSgd);
    when(fxRates.findOrNew("SGD", "USD", LocalDate.parse("2026-06-26"))).thenReturn(sgdUsd);
    when(fxRates.findOrNew("EUR", "USD", LocalDate.parse("2026-06-26"))).thenReturn(eurUsd);
    when(fxRateProvider.dailyRates("USD", Set.of("SGD", "EUR"), LocalDate.parse("2026-06-26")))
        .thenReturn(
            List.of(
                new FxRateProvider.ProviderFxRate(
                    "USD", "SGD", LocalDate.parse("2026-06-26"), new BigDecimal("1.35"))));
    when(fxRateProvider.dailyRates("SGD", Set.of("USD", "EUR"), LocalDate.parse("2026-06-26")))
        .thenReturn(
            List.of(
                new FxRateProvider.ProviderFxRate(
                    "SGD", "USD", LocalDate.parse("2026-06-26"), new BigDecimal("0.74"))));
    when(fxRateProvider.dailyRates("EUR", Set.of("USD", "SGD"), LocalDate.parse("2026-06-26")))
        .thenReturn(
            List.of(
                new FxRateProvider.ProviderFxRate(
                    "EUR", "USD", LocalDate.parse("2026-06-26"), new BigDecimal("1.08"))));

    job.refresh();

    verify(fxRateProvider).dailyRates("USD", Set.of("SGD", "EUR"), LocalDate.parse("2026-06-26"));
    verify(fxRateProvider).dailyRates("SGD", Set.of("USD", "EUR"), LocalDate.parse("2026-06-26"));
    verify(fxRateProvider).dailyRates("EUR", Set.of("USD", "SGD"), LocalDate.parse("2026-06-26"));
    assertEquals(new BigDecimal("1.35"), usdSgd.rate);
    assertEquals("fx-provider", usdSgd.source);
    assertEquals(false, usdSgd.stale);
    verify(fxRates).persist(usdSgd);
    verify(fxRates).persist(sgdUsd);
    verify(fxRates).persist(eurUsd);
  }

  private void mockCurrencies(List<String> instrumentResult, List<String> userResult) {
    when(entityManager.createQuery("select distinct i.currency from Instrument i", String.class))
        .thenReturn(instrumentCurrencies);
    when(entityManager.createQuery("select distinct u.baseCurrency from AppUser u", String.class))
        .thenReturn(userCurrencies);
    when(instrumentCurrencies.getResultList()).thenReturn(instrumentResult);
    when(userCurrencies.getResultList()).thenReturn(userResult);
  }

  private FxRefreshJob newSpyJob() {
    var spyJob = Mockito.spy(new FxRefreshJob());
    spyJob.fxRateProvider = fxRateProvider;
    spyJob.fxRates = fxRates;
    spyJob.entityManager = entityManager;
    spyJob.clock = Clock.fixed(Instant.parse("2026-06-26T00:00:00Z"), ZoneOffset.UTC);
    spyJob.defaultBaseCurrency = "usd";
    spyJob.enabled = true;
    return spyJob;
  }
}
