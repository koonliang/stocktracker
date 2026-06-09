package com.stocktracker.service;

import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.RoundingMode;
import java.util.StringJoiner;

@ApplicationScoped
public class TransactionExportService {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject CurrentUser currentUser;

  public String exportCsv() {
    var joiner = new StringJoiner("\n");
    joiner.add("date,ticker,type,quantity,price,fees");
    for (var transaction : transactionRepository.listAscending(currentUser.id())) {
      joiner.add(
          String.join(
              ",",
              transaction.tradeDate.toString(),
              transaction.instrumentSymbol,
              transaction.transactionType,
              transaction
                  .quantity
                  .setScale(6, RoundingMode.HALF_UP)
                  .stripTrailingZeros()
                  .toPlainString(),
              transaction
                  .price
                  .setScale(4, RoundingMode.HALF_UP)
                  .stripTrailingZeros()
                  .toPlainString(),
              transaction
                  .fees
                  .setScale(4, RoundingMode.HALF_UP)
                  .stripTrailingZeros()
                  .toPlainString()));
    }
    return joiner.toString() + "\n";
  }
}
