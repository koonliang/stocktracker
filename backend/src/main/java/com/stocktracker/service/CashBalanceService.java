package com.stocktracker.service;

import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CashBalanceService {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject CurrencyService currencyService;
  @Inject CurrentUser currentUser;

  public Map<String, BigDecimal> balancesForCurrentUser() {
    return balances(transactionRepository.listAscending(currentUser.id()));
  }

  public Map<String, BigDecimal> balances(List<PortfolioTransaction> transactions) {
    var balances = new LinkedHashMap<String, BigDecimal>();
    for (var transaction : transactions) {
      var currency = currency(transaction);
      if (currency == null) {
        continue;
      }
      balances.merge(currency, effect(transaction), BigDecimal::add);
    }
    return balances;
  }

  public ConvertedTotal baseConvertedTotal(
      Map<String, BigDecimal> balances, String baseCurrency, LocalDate onDate) {
    var total = BigDecimal.ZERO;
    var stale = false;
    for (var entry : balances.entrySet()) {
      var converted = currencyService.convert(entry.getValue(), entry.getKey(), baseCurrency, onDate);
      total = total.add(converted.value());
      stale = stale || converted.stale();
    }
    return new ConvertedTotal(total, stale);
  }

  private BigDecimal effect(PortfolioTransaction transaction) {
    var amount = transaction.amount == null ? BigDecimal.ZERO : transaction.amount;
    var fees = transaction.fees == null ? BigDecimal.ZERO : transaction.fees;
    return switch (transaction.transactionType) {
      case "deposit" -> amount;
      case "withdrawal", "fee" -> amount.negate();
      case "dividend" -> amount.subtract(fees);
      case "buy" -> transaction.quantity.multiply(transaction.price).add(fees).negate();
      case "sell" -> transaction.quantity.multiply(transaction.price).subtract(fees);
      default -> BigDecimal.ZERO;
    };
  }

  private String currency(PortfolioTransaction transaction) {
    if (transaction.currency != null && !transaction.currency.isBlank()) {
      return transaction.currency;
    }
    if (transaction.instrumentSymbol == null) {
      return null;
    }
    return instrumentRepository
        .findBySymbol(transaction.instrumentSymbol)
        .map(instrument -> instrument.currency)
        .orElse(null);
  }

  public record ConvertedTotal(BigDecimal value, boolean stale) {}
}
