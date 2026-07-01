package com.stocktracker.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.service.provider.ProviderConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ReferenceDataBootstrap {
  @Inject InstrumentRepository instrumentRepository;
  @Inject ObjectMapper objectMapper;
  @Inject EntityManager entityManager;
  @Inject ProviderConfig providerConfig;

  @Inject
  @ConfigProperty(name = "stocktracker.dev-bootstrap.enabled", defaultValue = "true")
  boolean enabled;

  @Transactional
  void onStart(@Observes @Priority(1) StartupEvent ignored) throws Exception {
    if (!enabled) {
      return;
    }
    var hasReferenceData = instrumentRepository.count() > 0;

    try (InputStream instrumentsStream = resource("seed/instruments.json");
        InputStream pricesStream = resource("seed/price-bars.json");
        InputStream statsStream = resource("seed/instrument-stats.json")) {
      var instruments =
          objectMapper.readValue(
              instrumentsStream, new TypeReference<java.util.List<Map<String, Object>>>() {});
      for (var row : instruments) {
        var symbol = row.get("symbol").toString().toUpperCase();
        if (instrumentRepository.existsSymbol(symbol)) {
          continue;
        }
        var instrument = new Instrument();
        instrument.symbol = symbol;
        instrument.name = row.get("name").toString();
        instrument.sector = row.get("sector").toString();
        instrument.exchange = row.get("exchange").toString();
        instrument.currency =
            row.get("currency") == null ? "USD" : row.get("currency").toString().toUpperCase();
        instrument.active = true;
        instrumentRepository.persist(instrument);
      }

      if (hasReferenceData || providerConfig.isLiveMarketDataProvider()) {
        return;
      }

      JsonNode prices = objectMapper.readTree(pricesStream);
      Iterator<Map.Entry<String, JsonNode>> priceFields = prices.fields();
      int inserted = 0;
      while (priceFields.hasNext()) {
        var entry = priceFields.next();
        for (JsonNode barNode : entry.getValue()) {
          var bar = new InstrumentPriceBar();
          bar.instrumentSymbol = entry.getKey();
          bar.tradeDate = LocalDate.parse(barNode.get("date").asText());
          bar.openPrice = decimal(barNode, "open");
          bar.highPrice = decimal(barNode, "high");
          bar.lowPrice = decimal(barNode, "low");
          bar.closePrice = decimal(barNode, "close");
          bar.volume = barNode.get("volume").asLong();
          persistPriceBar(bar);
          // Flush and detach periodically so the price-bar dataset (tens of
          // thousands of rows) does not accumulate in the persistence context.
          if (++inserted % 1000 == 0) {
            entityManager.flush();
            entityManager.clear();
          }
        }
      }
      entityManager.flush();
      entityManager.clear();

      JsonNode stats = objectMapper.readTree(statsStream);
      Iterator<Map.Entry<String, JsonNode>> statFields = stats.fields();
      while (statFields.hasNext()) {
        var entry = statFields.next();
        var node = entry.getValue();
        var stat = new InstrumentStat();
        stat.instrumentSymbol = entry.getKey();
        stat.openPrice = decimal(node, "open");
        stat.highPrice = decimal(node, "high");
        stat.lowPrice = decimal(node, "low");
        stat.previousClose = decimal(node, "previousClose");
        stat.volume = node.get("volume").asLong();
        stat.week52High = decimal(node, "week52High");
        stat.week52Low = decimal(node, "week52Low");
        stat.marketCap = node.get("marketCap").asLong();
        stat.peRatio = node.get("peRatio").isNull() ? null : decimal(node, "peRatio");
        stat.asOfDate = latestTradeDate(entry.getKey());
        persistInstrumentStat(stat);
      }
    }
  }

  InputStream resource(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
  }

  LocalDate latestTradeDate(String symbol) {
    return instrumentRepository.listPriceBars(symbol).stream()
        .map(bar -> bar.tradeDate)
        .max(LocalDate::compareTo)
        .orElse(LocalDate.now());
  }

  BigDecimal decimal(JsonNode node, String field) {
    return new BigDecimal(node.get(field).asText());
  }

  void persistPriceBar(InstrumentPriceBar bar) {
    bar.persist();
  }

  void persistInstrumentStat(InstrumentStat stat) {
    stat.persist();
  }
}
