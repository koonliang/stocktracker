package com.stocktracker.persistence;

import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class InstrumentRepository implements PanacheRepository<Instrument> {
  public Optional<Instrument> findBySymbol(String symbol) {
    return find("upper(symbol) = ?1", symbol.toUpperCase()).firstResultOptional();
  }

  public boolean existsSymbol(String symbol) {
    return count("upper(symbol) = ?1", symbol.toUpperCase()) > 0;
  }

  /** Active instruments whose symbol or name contains the query, capped for search results. */
  public List<Instrument> search(String query, int limit) {
    var like = "%" + query.trim().toLowerCase() + "%";
    return find("active = true and (lower(symbol) like ?1 or lower(name) like ?1)", like)
        .page(0, limit)
        .list();
  }

  public Map<String, Instrument> findBySymbols(Collection<String> symbols) {
    if (symbols.isEmpty()) {
      return Map.of();
    }
    return list("symbol in ?1", symbols).stream()
        .collect(Collectors.toMap(instrument -> instrument.symbol, Function.identity()));
  }

  public List<InstrumentPriceBar> listPriceBars(String symbol) {
    return InstrumentPriceBar.list(
        "instrumentSymbol = ?1 order by tradeDate", symbol.toUpperCase());
  }

  public Optional<InstrumentPriceBar> findPriceBar(String symbol, java.time.LocalDate tradeDate) {
    return InstrumentPriceBar.find(
            "instrumentSymbol = ?1 and tradeDate = ?2", symbol.toUpperCase(), tradeDate)
        .firstResultOptional();
  }

  public List<InstrumentPriceBar> listPriceBars(Collection<String> symbols) {
    if (symbols.isEmpty()) {
      return List.of();
    }
    return InstrumentPriceBar.list(
        "instrumentSymbol in ?1 order by instrumentSymbol, tradeDate", symbols);
  }

  public Optional<InstrumentStat> findStat(String symbol) {
    return InstrumentStat.find("instrumentSymbol", symbol.toUpperCase()).firstResultOptional();
  }
}
