package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TransactionExportServiceTest {
  private final PortfolioTransactionRepository transactions =
      Mockito.mock(PortfolioTransactionRepository.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private final TransactionCurrencyBackfillService backfillService =
      Mockito.mock(TransactionCurrencyBackfillService.class);
  private TransactionExportService service;

  @BeforeEach
  void setUp() {
    service = new TransactionExportService();
    service.transactionRepository = transactions;
    service.currentUser = currentUser;
    service.transactionCurrencyBackfillService = backfillService;
  }

  @Test
  void exportsCsvAndBackfillsBeforeReading() {
    when(currentUser.id()).thenReturn(1L);
    when(transactions.listAscending(1L))
        .thenReturn(
            List.of(
                transaction("buy", "AAPL", "2", "10.5", "1", null, "USD"),
                transaction("deposit", null, null, null, null, "1000", "SGD")));

    var csv = service.exportCsv();

    verify(backfillService).backfillCurrentUser();
    assertTrue(csv.startsWith("date,ticker,type,quantity,price,fees,amount,currency\n"));
    assertTrue(csv.contains("2025-01-02,AAPL,buy,2,10.5,1,22,USD"));
    assertTrue(csv.contains("2025-01-02,,deposit,,,,1000,SGD"));
    assertTrue(csv.endsWith("\n"));
  }

  private PortfolioTransaction transaction(
      String type,
      String symbol,
      String quantity,
      String price,
      String fees,
      String amount,
      String currency) {
    var tx = new PortfolioTransaction();
    tx.tradeDate = LocalDate.of(2025, 1, 2);
    tx.transactionType = type;
    tx.instrumentSymbol = symbol;
    tx.quantity = quantity == null ? null : new BigDecimal(quantity);
    tx.price = price == null ? null : new BigDecimal(price);
    tx.fees = fees == null ? null : new BigDecimal(fees);
    tx.amount = amount == null ? null : new BigDecimal(amount);
    tx.currency = currency;
    return tx;
  }
}
