package com.stocktracker.service;

import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringJoiner;

@ApplicationScoped
public class TransactionExportService {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject CurrentUser currentUser;
  @Inject TransactionCurrencyBackfillService transactionCurrencyBackfillService;

  public String exportCsv() {
    transactionCurrencyBackfillService.backfillCurrentUser();
    var joiner = new StringJoiner("\n");
    joiner.add("date,ticker,type,quantity,price,fees,amount,currency");
    for (var transaction : transactionRepository.listAscending(currentUser.id())) {
      var amount = transaction.amount;
      if (amount == null && transaction.quantity != null && transaction.price != null) {
        var raw = transaction.quantity.multiply(transaction.price);
        if ("buy".equals(transaction.transactionType)) {
          amount = transaction.fees == null ? raw : raw.add(transaction.fees);
        } else if ("sell".equals(transaction.transactionType)) {
          amount = transaction.fees == null ? raw : raw.subtract(transaction.fees);
        }
      }
      joiner.add(
          String.join(
              ",",
              transaction.tradeDate.toString(),
              transaction.instrumentSymbol == null ? "" : transaction.instrumentSymbol,
              transaction.transactionType,
              format(transaction.quantity, 6),
              format(transaction.price, 4),
              format(transaction.fees, 4),
              format(amount, 4),
              transaction.currency == null ? "" : transaction.currency));
    }
    return joiner.toString() + "\n";
  }

  private String format(BigDecimal value, int scale) {
    if (value == null) {
      return "";
    }
    return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }
}
