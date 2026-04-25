package com.stocktracker.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DevDataBootstrap {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject ObjectMapper objectMapper;
  @ConfigProperty(name = "stocktracker.dev-bootstrap.enabled", defaultValue = "true")
  boolean enabled;

  @Transactional
  void onStart(@Observes StartupEvent ignored) throws Exception {
    if (!enabled) {
      return;
    }
    if (transactionRepository.count() > 0) {
      return;
    }
    try (InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("seed/demo-transactions.json")) {
      var rows =
          objectMapper.readValue(stream, new TypeReference<List<Map<String, Object>>>() {});
      for (var row : rows) {
        var transaction = new PortfolioTransaction();
        transaction.id = UUID.fromString(row.get("id").toString());
        transaction.tradeDate = LocalDate.parse(row.get("date").toString());
        transaction.instrumentSymbol = row.get("ticker").toString();
        transaction.transactionType = row.get("type").toString();
        transaction.quantity = new BigDecimal(row.get("quantity").toString());
        transaction.price = new BigDecimal(row.get("price").toString());
        transaction.fees = new BigDecimal(row.get("fees").toString());
        transaction.source = "MANUAL";
        transactionRepository.persist(transaction);
      }
    }
  }
}
