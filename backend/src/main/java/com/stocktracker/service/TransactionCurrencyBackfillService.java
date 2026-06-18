package com.stocktracker.service;

import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TransactionCurrencyBackfillService {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject CurrentUser currentUser;

  @ConfigProperty(name = "stocktracker.base-currency.default", defaultValue = "USD")
  String defaultBaseCurrency;

  @Transactional
  public long backfillCurrentUser() {
    var user = currentUser.require();
    var baseCurrency = user.baseCurrency == null ? defaultBaseCurrency : user.baseCurrency;
    long count = 0;
    for (var transaction : transactionRepository.findMissingCurrency(user.id)) {
      backfill(transaction, baseCurrency);
      count++;
    }
    return count;
  }

  public void backfill(PortfolioTransaction transaction, String baseCurrency) {
    if (transaction.currency != null) {
      return;
    }
    if (transaction.instrumentSymbol != null) {
      transaction.currency =
          instrumentRepository
              .findBySymbol(transaction.instrumentSymbol)
              .map(instrument -> instrument.currency)
              .orElse(baseCurrency);
      transaction.currencySource = "instrument";
    } else {
      transaction.currency = baseCurrency;
      transaction.currencySource = "user_base_backfill";
    }
    transaction.currencyBackfilledAt = LocalDateTime.now();
  }
}
