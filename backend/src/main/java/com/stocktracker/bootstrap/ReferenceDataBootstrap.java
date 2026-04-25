package com.stocktracker.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.persistence.InstrumentRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;

@ApplicationScoped
public class ReferenceDataBootstrap {
  @Inject InstrumentRepository instrumentRepository;
  @Inject ObjectMapper objectMapper;

  @Transactional
  void onStart(@Observes StartupEvent ignored) throws Exception {
    if (instrumentRepository.count() > 0) {
      return;
    }

    try (InputStream instrumentsStream = resource("seed/instruments.json");
        InputStream pricesStream = resource("seed/price-bars.json");
        InputStream statsStream = resource("seed/instrument-stats.json")) {
      var instruments =
          objectMapper.readValue(
              instrumentsStream, new TypeReference<java.util.List<Map<String, Object>>>() {});
      for (var row : instruments) {
        var instrument = new Instrument();
        instrument.symbol = row.get("symbol").toString().toUpperCase();
        instrument.name = row.get("name").toString();
        instrument.sector = row.get("sector").toString();
        instrument.exchange = row.get("exchange").toString();
        instrument.active = true;
        instrumentRepository.persist(instrument);
      }

      JsonNode prices = objectMapper.readTree(pricesStream);
      Iterator<Map.Entry<String, JsonNode>> priceFields = prices.fields();
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
          bar.persist();
        }
      }

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
        stat.persist();
      }
    }
  }

  private InputStream resource(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
  }

  private LocalDate latestTradeDate(String symbol) {
    return instrumentRepository.listPriceBars(symbol).stream()
        .map(bar -> bar.tradeDate)
        .max(LocalDate::compareTo)
        .orElse(LocalDate.now());
  }

  private BigDecimal decimal(JsonNode node, String field) {
    return new BigDecimal(node.get(field).asText());
  }
}
