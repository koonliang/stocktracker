package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TransactionCurrencyBackfillServiceTest {
  private final PortfolioTransactionRepository transactions =
      Mockito.mock(PortfolioTransactionRepository.class);
  private final InstrumentRepository instruments = Mockito.mock(InstrumentRepository.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private TransactionCurrencyBackfillService service;

  @BeforeEach
  void setUp() {
    service = new TransactionCurrencyBackfillService();
    service.transactionRepository = transactions;
    service.instrumentRepository = instruments;
    service.currentUser = currentUser;
    service.defaultBaseCurrency = "USD";
  }

  @Test
  void backfillUsesInstrumentCurrencyWhenPresent() {
    var tx = new PortfolioTransaction();
    tx.instrumentSymbol = "AAPL";
    when(instruments.findBySymbol("AAPL")).thenReturn(Optional.of(instrument("USD")));

    service.backfill(tx, "SGD");

    assertEquals("USD", tx.currency);
    assertEquals("instrument", tx.currencySource);
    assertNotNull(tx.currencyBackfilledAt);
  }

  @Test
  void backfillUsesBaseCurrencyForCashTransactions() {
    var tx = new PortfolioTransaction();

    service.backfill(tx, "SGD");

    assertEquals("SGD", tx.currency);
    assertEquals("user_base_backfill", tx.currencySource);
  }

  @Test
  void backfillCurrentUserCountsMissingTransactions() {
    var user = new AppUser();
    user.id = 5L;
    user.baseCurrency = null;
    var security = new PortfolioTransaction();
    security.instrumentSymbol = "AAPL";
    var cash = new PortfolioTransaction();
    when(currentUser.require()).thenReturn(user);
    when(transactions.findMissingCurrency(5L)).thenReturn(List.of(security, cash));
    when(instruments.findBySymbol("AAPL")).thenReturn(Optional.of(instrument("USD")));

    var count = service.backfillCurrentUser();

    assertEquals(2L, count);
    assertEquals("USD", security.currency);
    assertEquals("USD", cash.currency);
  }

  private Instrument instrument(String currency) {
    var instrument = new Instrument();
    instrument.currency = currency;
    return instrument;
  }
}
