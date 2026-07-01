package com.stocktracker.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.service.provider.ProviderConfig;
import jakarta.enterprise.inject.Vetoed;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReferenceDataBootstrapTest {
  private final InstrumentRepository instrumentRepository =
      Mockito.mock(InstrumentRepository.class);
  private final EntityManager entityManager = Mockito.mock(EntityManager.class);
  private final ProviderConfig providerConfig = Mockito.mock(ProviderConfig.class);

  private TestReferenceDataBootstrap bootstrap;

  @BeforeEach
  void setUp() {
    bootstrap = new TestReferenceDataBootstrap();
    bootstrap.instrumentRepository = instrumentRepository;
    bootstrap.objectMapper = new ObjectMapper();
    bootstrap.entityManager = entityManager;
    bootstrap.providerConfig = providerConfig;
    bootstrap.enabled = true;
    Mockito.doAnswer(
            invocation -> {
              bootstrap.persistedInstruments.add(invocation.getArgument(0, Instrument.class));
              return null;
            })
        .when(instrumentRepository)
        .persist(Mockito.any(Instrument.class));
  }

  @Test
  void onStartSkipsWhenDisabled() throws Exception {
    bootstrap.enabled = false;

    bootstrap.onStart(null);

    verify(instrumentRepository, never()).count();
  }

  @Test
  void onStartPersistsInstrumentsBarsAndStatsForStubMode() throws Exception {
    when(instrumentRepository.count()).thenReturn(0L);
    when(instrumentRepository.existsSymbol("AAPL")).thenReturn(false);
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(priceBar(LocalDate.parse("2026-06-25"))));
    bootstrap.resources.put(
        "seed/instruments.json",
        """
        [{"symbol":"aapl","name":"Apple","sector":"Tech","exchange":"NASDAQ","currency":null}]
        """);
    bootstrap.resources.put(
        "seed/price-bars.json",
        """
        {"AAPL":[{"date":"2026-06-25","open":"10","high":"11","low":"9","close":"10.5","volume":123}]}
        """);
    bootstrap.resources.put(
        "seed/instrument-stats.json",
        """
        {"AAPL":{"open":"10","high":"11","low":"9","previousClose":"10","volume":123,"week52High":"20","week52Low":"5","marketCap":1000,"peRatio":null}}
        """);

    bootstrap.onStart(null);

    assertEquals(1, bootstrap.persistedBars.size());
    assertEquals("AAPL", bootstrap.persistedBars.getFirst().instrumentSymbol);
    assertEquals(1, bootstrap.persistedStats.size());
    assertEquals("USD", bootstrap.persistedInstruments.getFirst().currency);
    assertNull(bootstrap.persistedStats.getFirst().peRatio);
    assertEquals(LocalDate.parse("2026-06-25"), bootstrap.persistedStats.getFirst().asOfDate);
    verify(entityManager).flush();
    verify(entityManager).clear();
  }

  @Test
  void onStartSkipsBarsAndStatsWhenReferenceDataAlreadyExists() throws Exception {
    when(instrumentRepository.count()).thenReturn(1L);
    when(instrumentRepository.existsSymbol("AAPL")).thenReturn(false);
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    bootstrap.resources.put(
        "seed/instruments.json",
        """
        [{"symbol":"AAPL","name":"Apple","sector":"Tech","exchange":"NASDAQ","currency":"USD"}]
        """);
    bootstrap.resources.put("seed/price-bars.json", "{\"AAPL\":[]}");
    bootstrap.resources.put("seed/instrument-stats.json", "{\"AAPL\":{}}");

    bootstrap.onStart(null);

    assertEquals(1, bootstrap.persistedInstruments.size());
    assertEquals(0, bootstrap.persistedBars.size());
    assertEquals(0, bootstrap.persistedStats.size());
  }

  @Test
  void onStartSkipsBarsAndStatsWhenLiveProviderIsEnabled() throws Exception {
    when(instrumentRepository.count()).thenReturn(0L);
    when(instrumentRepository.existsSymbol("AAPL")).thenReturn(true);
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(true);
    bootstrap.resources.put(
        "seed/instruments.json",
        """
        [{"symbol":"AAPL","name":"Apple","sector":"Tech","exchange":"NASDAQ","currency":"USD"}]
        """);
    bootstrap.resources.put("seed/price-bars.json", "{\"AAPL\":[]}");
    bootstrap.resources.put("seed/instrument-stats.json", "{\"AAPL\":{}}");

    bootstrap.onStart(null);

    assertEquals(0, bootstrap.persistedBars.size());
    assertEquals(0, bootstrap.persistedStats.size());
    verify(entityManager, never()).flush();
  }

  private InstrumentPriceBar priceBar(LocalDate date) {
    var bar = new InstrumentPriceBar();
    bar.tradeDate = date;
    return bar;
  }

  @Vetoed
  private static final class TestReferenceDataBootstrap extends ReferenceDataBootstrap {
    private final Map<String, String> resources = new HashMap<>();
    private final List<com.stocktracker.domain.Instrument> persistedInstruments = new ArrayList<>();
    private final List<InstrumentPriceBar> persistedBars = new ArrayList<>();
    private final List<InstrumentStat> persistedStats = new ArrayList<>();

    @Override
    InputStream resource(String name) {
      return new ByteArrayInputStream(resources.get(name).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    void persistPriceBar(InstrumentPriceBar bar) {
      persistedBars.add(bar);
    }

    @Override
    void persistInstrumentStat(InstrumentStat stat) {
      persistedStats.add(stat);
    }
  }
}
