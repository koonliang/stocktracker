package com.stocktracker.persistence;

import com.stocktracker.domain.InstrumentQuote;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class QuoteRepository implements PanacheRepositoryBase<InstrumentQuote, String> {
  public Optional<InstrumentQuote> findBySymbol(String symbol) {
    return findByIdOptional(symbol.toUpperCase());
  }

  public List<InstrumentQuote> findBySymbols(Collection<String> symbols) {
    if (symbols.isEmpty()) {
      return List.of();
    }
    return list("instrumentSymbol in ?1", symbols);
  }

  /** The existing (managed) quote row for a symbol, or a new transient one to be persisted. */
  public InstrumentQuote findOrNew(String symbol) {
    return findBySymbol(symbol)
        .orElseGet(
            () -> {
              var quote = new InstrumentQuote();
              quote.instrumentSymbol = symbol.toUpperCase();
              return quote;
            });
  }
}
