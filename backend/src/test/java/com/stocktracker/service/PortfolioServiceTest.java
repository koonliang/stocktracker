package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.dto.TransactionRequest;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PortfolioServiceTest {
  private final PortfolioTransactionRepository transactionRepository =
      Mockito.mock(PortfolioTransactionRepository.class);
  private final InstrumentRepository instrumentRepository = Mockito.mock(InstrumentRepository.class);
  private final TransactionValidationService transactionValidationService =
      Mockito.mock(TransactionValidationService.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private final QuoteCacheService quoteCacheService = Mockito.mock(QuoteCacheService.class);
  private final CurrencyService currencyService = Mockito.mock(CurrencyService.class);
  private final CostBasisEngine costBasisEngine = new CostBasisEngine();
  private final TransactionCurrencyBackfillService transactionCurrencyBackfillService =
      Mockito.mock(TransactionCurrencyBackfillService.class);
  private final OnDemandFxService onDemandFxService = Mockito.mock(OnDemandFxService.class);
  private final PortfolioService self = Mockito.mock(PortfolioService.class);

  private PortfolioService service;

  @BeforeEach
  void setUp() {
    service = new PortfolioService();
    service.transactionRepository = transactionRepository;
    service.instrumentRepository = instrumentRepository;
    service.transactionValidationService = transactionValidationService;
    service.currentUser = currentUser;
    service.quoteCacheService = quoteCacheService;
    service.currencyService = currencyService;
    service.costBasisEngine = costBasisEngine;
    service.transactionCurrencyBackfillService = transactionCurrencyBackfillService;
    service.onDemandFxService = onDemandFxService;
    service.self = self;
    service.defaultBaseCurrency = "USD";
  }

  @Test
  void currentShareBalancesTracksBuysSellsAndSplits() {
    when(currentUser.id()).thenReturn(7L);
    when(transactionRepository.listAscending(7L))
        .thenReturn(
            List.of(
                transaction("buy", "AAPL", "10", "100", "0", null, "USD"),
                transaction("dividend", "AAPL", "0", "0", "0", "15", "USD"),
                transaction("sell", "AAPL", "3", "120", "0", null, "USD"),
                transaction("split", "AAPL", "2", "0", "0", null, "USD"),
                transaction("deposit", null, null, null, null, "1000", "USD")));

    var balances = service.currentShareBalances();

    assertEquals(Set.of("AAPL"), balances.keySet());
    assertEquals(0, balances.get("AAPL").compareTo(new BigDecimal("14")));
  }

  @Test
  void fxSourceCurrencyUsesInstrumentCurrencyForSecuritiesAndRequestCurrencyForCash() {
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument("AAPL", "USD")));
    when(instrumentRepository.findBySymbol("MISSING")).thenReturn(Optional.empty());

    assertEquals(
        "USD",
        service.fxSourceCurrency(
            new TransactionRequest(
                LocalDate.of(2026, 1, 10),
                "AAPL",
                "buy",
                new BigDecimal("1"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                null)));
    assertEquals(
        "SGD",
        service.fxSourceCurrency(
            new TransactionRequest(
                LocalDate.of(2026, 1, 10),
                null,
                "deposit",
                null,
                null,
                null,
                new BigDecimal("500"),
                "SGD")));
    assertEquals(
        null,
        service.fxSourceCurrency(
            new TransactionRequest(
                LocalDate.of(2026, 1, 10),
                "MISSING",
                "sell",
                new BigDecimal("1"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                null)));
  }

  @Test
  void preflightHistoricalFxUsesUserBaseCurrencyAndSkipsRequestsWithoutConvertibleSource() {
    var user = new AppUser();
    user.baseCurrency = "sgd";
    when(currentUser.optional()).thenReturn(Optional.of(user));
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument("AAPL", "USD")));

    service.preflightHistoricalFx(
        List.of(
            new TransactionRequest(
                LocalDate.of(2026, 2, 3),
                "AAPL",
                "buy",
                new BigDecimal("2"),
                new BigDecimal("50"),
                BigDecimal.ZERO,
                null,
                null),
            new TransactionRequest(
                LocalDate.of(2026, 2, 4),
                null,
                "deposit",
                null,
                null,
                null,
                new BigDecimal("1000"),
                "EUR"),
            new TransactionRequest(
                null,
                "AAPL",
                "buy",
                new BigDecimal("1"),
                new BigDecimal("55"),
                BigDecimal.ZERO,
                null,
                null),
            new TransactionRequest(
                LocalDate.of(2026, 2, 5),
                "AAPL",
                "fee",
                null,
                null,
                null,
                new BigDecimal("5"),
                null)));

    verify(onDemandFxService).ensureRate("USD", "SGD", LocalDate.of(2026, 2, 3));
    verify(onDemandFxService).ensureRate("EUR", "SGD", LocalDate.of(2026, 2, 4));
    verify(onDemandFxService, never()).ensureRate(any(), any(), eq(LocalDate.of(2026, 2, 5)));
  }

  @Test
  void createTransactionsTransactionalBackfillsMissingCurrencyAndMarksProvidedCurrency() {
    var user = new AppUser();
    user.id = 42L;
    user.baseCurrency = "SGD";
    when(currentUser.id()).thenReturn(42L);
    when(currentUser.optional()).thenReturn(Optional.of(user));
    when(self.currentShareBalances()).thenReturn(Map.of());

    var missingCurrency =
        new TransactionRequest(
            LocalDate.of(2026, 3, 1),
            "AAPL",
            "buy",
            new BigDecimal("2"),
            new BigDecimal("100"),
            new BigDecimal("1"),
            null,
            null);
    var providedCurrency =
        new TransactionRequest(
            LocalDate.of(2026, 3, 2),
            null,
            "deposit",
            null,
            null,
            null,
            new BigDecimal("200"),
            "USD");

    service.createTransactionsTransactional(List.of(missingCurrency, providedCurrency), "manual");

    verify(transactionValidationService).validateBatch(List.of(missingCurrency, providedCurrency), Map.of());

    var transactionCaptor = ArgumentCaptor.forClass(PortfolioTransaction.class);
    verify(transactionRepository, Mockito.times(2)).persist(transactionCaptor.capture());
    var persisted = transactionCaptor.getAllValues();

    assertEquals(42L, persisted.get(0).userId);
    assertEquals("manual", persisted.get(0).source);
    assertEquals(BigDecimal.ZERO, persisted.get(1).quantity);
    assertEquals(BigDecimal.ZERO, persisted.get(1).price);
    assertEquals(BigDecimal.ZERO, persisted.get(1).fees);
    assertEquals("provided", persisted.get(1).currencySource);

    verify(transactionCurrencyBackfillService).backfill(persisted.get(0), "SGD");
    verify(transactionCurrencyBackfillService, never()).backfill(eq(persisted.get(1)), any());
  }

  @Test
  void buildDashboardAggregatesConvertedHoldingSummary() {
    var user = new AppUser();
    user.baseCurrency = "SGD";
    when(currentUser.optional()).thenReturn(Optional.of(user));

    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("15");
    quote.previousClose = new BigDecimal("12");
    quote.asOf = Instant.parse("2026-03-05T09:30:00Z");
    quote.fetchedAt = Instant.parse("2026-03-05T09:31:00Z");

    when(instrumentRepository.findBySymbols(Set.of("AAPL")))
        .thenReturn(Map.of("AAPL", instrument("AAPL", "USD")));
    when(instrumentRepository.listPriceBars(Set.of("AAPL"))).thenReturn(List.of());
    when(quoteCacheService.cachedBySymbol(Set.of("AAPL"))).thenReturn(Map.of("AAPL", quote));
    when(quoteCacheService.effectiveStale(quote)).thenReturn(false);
    when(currencyService.convertHolding(
            ArgumentMatchers.any(BigDecimal.class), eq("USD"), eq("SGD"), any(LocalDate.class)))
        .thenAnswer(
            invocation -> {
              var amount = invocation.<BigDecimal>getArgument(0);
              var date = invocation.<LocalDate>getArgument(3);
              if (amount.compareTo(new BigDecimal("21")) == 0) {
                return new CurrencyService.Converted(
                    new BigDecimal("28.3500"), date, FxStatus.current);
              }
              if (amount.compareTo(new BigDecimal("30")) == 0) {
                return new CurrencyService.Converted(
                    new BigDecimal("40.5000"), date, FxStatus.current);
              }
              if (amount.compareTo(new BigDecimal("15")) == 0) {
                return new CurrencyService.Converted(
                    new BigDecimal("20.2500"), date, FxStatus.current);
              }
              if (amount.compareTo(new BigDecimal("6")) == 0) {
                return new CurrencyService.Converted(
                    new BigDecimal("8.1000"), date, FxStatus.current);
              }
              return null;
            });

    var dashboard =
        service.buildDashboard(
            List.of(transaction("buy", "AAPL", "2", "10", "1", null, "USD")));

    assertEquals("SGD", dashboard.summary().baseCurrency());
    assertEquals(40.5, dashboard.summary().totalMarketValue());
    assertEquals(28.35, dashboard.summary().totalCostBasis());
    assertEquals(12.15, dashboard.summary().totalUnrealizedPnL());
    assertEquals(8.1, dashboard.summary().totalDayChange());
    assertTrue(dashboard.warnings().isEmpty());
    assertEquals(1, dashboard.holdings().size());

    var holding = dashboard.holdings().getFirst();
    assertEquals("AAPL", holding.ticker());
    assertEquals(2.0, holding.shares());
    assertEquals(15.0, holding.nativePrice());
    assertEquals(40.5, holding.marketValue());
    assertEquals(1.0, holding.weight());
    assertNotNull(holding.asOf());
    assertNotNull(holding.fetchedAt());
    assertFalse(holding.stale());
  }

  @Test
  void getDashboardAndListTransactionsBackfillBeforeReading() {
    when(currentUser.id()).thenReturn(2L);
    when(currentUser.optional()).thenReturn(Optional.empty());
    when(transactionRepository.listAscending(2L)).thenReturn(List.of());
    var tx = transaction("deposit", null, "0", "0", "0", "10", "USD");
    tx.id = 1L;
    when(transactionRepository.listDescending(2L)).thenReturn(List.of(tx));

    var dashboard = service.getDashboard();
    var transactions = service.listTransactions();

    verify(transactionCurrencyBackfillService, Mockito.times(2)).backfillCurrentUser();
    assertEquals(0, dashboard.holdings().size());
    assertEquals(1, transactions.size());
  }

  @Test
  void createTransactionsNormalizesAndDelegatesToTransactionalSelf() {
    var request =
        new TransactionRequest(
            LocalDate.of(2026, 3, 1), " aapl ", " BUY ", new BigDecimal("2.0"), new BigDecimal("10.0"), BigDecimal.ZERO, null, " usd ");
    var normalized =
        new TransactionRequest(
            LocalDate.of(2026, 3, 1), "AAPL", "buy", new BigDecimal("2"), new BigDecimal("10"), BigDecimal.ZERO, null, "USD");
    when(transactionValidationService.normalize(request)).thenReturn(normalized);
    when(currentUser.optional()).thenReturn(Optional.empty());
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument("AAPL", "USD")));

    service.createTransactions(List.of(request), "manual");

    verify(self).createTransactionsTransactional(List.of(normalized), "manual");
    verify(onDemandFxService).ensureRate("USD", "USD", LocalDate.of(2026, 3, 1));
  }

  @Test
  void deleteTransactionRejectsUnknownId() {
    when(currentUser.id()).thenReturn(1L);
    when(transactionRepository.findByIdAndUser(99L, 1L)).thenReturn(Optional.empty());

    var error = org.junit.jupiter.api.Assertions.assertThrows(
        com.stocktracker.api.ApiException.class, () -> service.deleteTransaction(99L));

    assertEquals("not_found", error.code());
  }

  @Test
  void buildDashboardReturnsEmptySummaryWithoutSymbolsAndFindPositionNullWithoutUser() {
    when(currentUser.optional()).thenReturn(Optional.empty());

    var dashboard = service.buildDashboard(List.of(transaction("deposit", null, null, null, null, "10", "USD")));

    assertEquals("USD", dashboard.summary().baseCurrency());
    assertEquals(0, dashboard.holdings().size());
    assertNull(service.findPosition("AAPL"));
  }

  @Test
  void buildDashboardMarksUnavailableFxAndSkipsSoldOutPositions() {
    var user = new AppUser();
    user.id = 5L;
    user.baseCurrency = "SGD";
    when(currentUser.optional()).thenReturn(Optional.of(user));
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("15");
    quote.previousClose = new BigDecimal("14");
    when(instrumentRepository.findBySymbols(Set.of("AAPL")))
        .thenReturn(Map.of("AAPL", instrument("AAPL", "USD")));
    when(instrumentRepository.listPriceBars(Set.of("AAPL"))).thenReturn(List.of());
    when(quoteCacheService.cachedBySymbol(Set.of("AAPL"))).thenReturn(Map.of("AAPL", quote));
    when(quoteCacheService.effectiveStale(quote)).thenReturn(true);
    when(currencyService.convertHolding(any(BigDecimal.class), eq("USD"), eq("SGD"), any(LocalDate.class)))
        .thenReturn(new CurrencyService.Converted(BigDecimal.ZERO, null, FxStatus.unavailable));

    var dashboard =
        service.buildDashboard(
            List.of(
                transaction("buy", "AAPL", "2", "10", "1", null, "USD"),
                transaction("sell", "AAPL", "2", "12", "0", null, "USD"),
                transaction("buy", "AAPL", "1", "11", "0", null, "USD")));

    assertEquals(1, dashboard.holdings().size());
    assertEquals(1, dashboard.warnings().size());
    assertTrue(dashboard.holdings().getFirst().stale());
    assertEquals(0.0, dashboard.summary().totalMarketValue());
  }

  private PortfolioTransaction transaction(
      String type,
      String symbol,
      String quantity,
      String price,
      String fees,
      String amount,
      String currency) {
    var transaction = new PortfolioTransaction();
    transaction.transactionType = type;
    transaction.instrumentSymbol = symbol;
    transaction.quantity = quantity == null ? null : new BigDecimal(quantity);
    transaction.price = price == null ? null : new BigDecimal(price);
    transaction.fees = fees == null ? null : new BigDecimal(fees);
    transaction.amount = amount == null ? null : new BigDecimal(amount);
    transaction.currency = currency;
    transaction.tradeDate = LocalDate.of(2026, 1, 1);
    return transaction;
  }

  private Instrument instrument(String symbol, String currency) {
    var instrument = new Instrument();
    instrument.symbol = symbol;
    instrument.name = symbol + " Inc";
    instrument.currency = currency;
    return instrument;
  }
}
