package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.api.ApiException;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PerformanceServiceTest {
  private final PortfolioTransactionRepository transactionRepository =
      Mockito.mock(PortfolioTransactionRepository.class);
  private final InstrumentRepository instrumentRepository = Mockito.mock(InstrumentRepository.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private final LotMatchingService lotMatchingService = Mockito.mock(LotMatchingService.class);
  private final CurrencyService currencyService = Mockito.mock(CurrencyService.class);
  private final HistoricalBackfillService historicalBackfillService =
      Mockito.mock(HistoricalBackfillService.class);
  private final FxHistoricalBackfillService fxHistoricalBackfillService =
      Mockito.mock(FxHistoricalBackfillService.class);

  @Test
  void timeWeightedReturnChainsSubPeriodReturns() {
    var factor = BigDecimal.ONE;
    factor =
        PerformanceService.nextTwrFactor(
            factor, new BigDecimal("100.00"), new BigDecimal("110.00"));
    factor =
        PerformanceService.nextTwrFactor(factor, new BigDecimal("110.00"), new BigDecimal("99.00"));

    assertEquals(
        0, PerformanceService.factorToPercent(factor).compareTo(new BigDecimal("-1.000000000000")));
  }

  @Test
  void contributionsReconcileToTotalWithinTolerance() {
    var first = PerformanceService.contributionPct(new BigDecimal("30"), new BigDecimal("100"));
    var second = PerformanceService.contributionPct(new BigDecimal("70"), new BigDecimal("100"));

    assertEquals(0, first.add(second).compareTo(new BigDecimal("100.00000000")));
  }

  @Test
  void performanceReturnsZeroSeriesWhenUserHasNoTransactions() {
    var service = service();
    var user = new AppUser();
    user.id = 4L;
    user.baseCurrency = "SGD";
    when(currentUser.require()).thenReturn(user);
    when(transactionRepository.listAscending(4L)).thenReturn(List.of());
    when(instrumentRepository.findBySymbols(java.util.Set.of())).thenReturn(Map.of());
    when(instrumentRepository.listPriceBars(java.util.Set.of())).thenReturn(List.of());
    when(lotMatchingService.match(List.of(), "fifo"))
        .thenReturn(new CostBasisEngine.Result(List.of(), List.of()));

    var response = service.performance(null, null);

    assertEquals("1Y", response.window());
    assertEquals("fifo", response.method());
    assertEquals("SGD", response.baseCurrency());
    assertEquals(0.0, response.realizedPnL());
    assertEquals(0.0, response.unrealizedPnL());
    assertEquals(2, response.returnSeries().size());
    assertEquals(0.0, response.timeWeightedReturnPct());
  }

  @Test
  void performanceAggregatesClosedLotsIncomeAndOpenLotContribution() {
    var service = service();
    var user = new AppUser();
    user.id = 9L;
    user.baseCurrency = "USD";
    var buy = tx("AAPL", "buy", "2026-06-01", "1", "100", null, "USD");
    var dividend = tx("AAPL", "dividend", "2026-06-15", "0", "0", "5", "USD");
    dividend.fees = new BigDecimal("1");
    var sell = tx("AAPL", "sell", "2026-06-20", "1", "120", null, "USD");
    var transactions = List.of(buy, dividend, sell);

    var openLot =
        new CostBasisEngine.Lot(
            "AAPL",
            LocalDate.parse("2026-06-21"),
            new BigDecimal("2"),
            new BigDecimal("180"),
            new BigDecimal("90"));
    var closedLot =
        new CostBasisEngine.ClosedLot(
            "AAPL",
            LocalDate.parse("2026-06-01"),
            LocalDate.parse("2026-06-20"),
            new BigDecimal("1"),
            new BigDecimal("100"),
            new BigDecimal("120"),
            new BigDecimal("20"));

    when(currentUser.require()).thenReturn(user);
    when(transactionRepository.listAscending(9L)).thenReturn(transactions);
    when(instrumentRepository.findBySymbols(java.util.Set.of("AAPL")))
        .thenReturn(Map.of("AAPL", instrument("AAPL", "USD")));
    when(instrumentRepository.listPriceBars(java.util.Set.of("AAPL")))
        .thenReturn(List.of(bar("AAPL", "2026-06-25", "100"), bar("AAPL", "2026-06-26", "110")));
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(bar("AAPL", "2026-06-25", "100"), bar("AAPL", "2026-06-26", "110")));
    when(lotMatchingService.match(any(List.class), eq("fifo")))
        .thenReturn(new CostBasisEngine.Result(List.of(openLot), List.of(closedLot)));
    when(currencyService.convertTransaction(new BigDecimal("20"), "USD", "USD", LocalDate.parse("2026-06-20")))
        .thenReturn(new CurrencyService.Converted(new BigDecimal("20"), LocalDate.parse("2026-06-20"), FxStatus.current));
    when(currencyService.convertTransaction(new BigDecimal("4"), "USD", "USD", LocalDate.parse("2026-06-15")))
        .thenReturn(new CurrencyService.Converted(new BigDecimal("4"), LocalDate.parse("2026-06-15"), FxStatus.current));
    when(currencyService.convertHolding(any(BigDecimal.class), eq("USD"), eq("USD"), any(LocalDate.class)))
        .thenAnswer(
            invocation ->
                new CurrencyService.Converted(
                    invocation.getArgument(0), invocation.getArgument(3), FxStatus.current));

    var response = service.performance("1M", "fifo");

    verify(fxHistoricalBackfillService)
        .backfillForBase(eq("USD"), eq(java.util.Set.of("USD")), eq(LocalDate.parse("2026-06-01")), any(LocalDate.class));
    assertEquals(24.0, response.realizedPnL());
    assertEquals(40.0, response.unrealizedPnL());
    assertEquals(1, response.closedLots().size());
    assertEquals(1, response.incomeEvents().size());
    assertEquals(1, response.contributions().size());
    assertEquals(100.0, response.contributions().getFirst().contributionPct());
  }

  @Test
  void performanceUsesDefaultBaseCurrencyAndAllWindow() {
    var service = service();
    var user = new AppUser();
    user.id = 1L;
    user.baseCurrency = null;
    when(currentUser.require()).thenReturn(user);
    when(transactionRepository.listAscending(1L)).thenReturn(List.of());
    when(instrumentRepository.findBySymbols(java.util.Set.of())).thenReturn(Map.of());
    when(instrumentRepository.listPriceBars(java.util.Set.of())).thenReturn(List.of());
    when(lotMatchingService.match(List.of(), "lifo"))
        .thenReturn(new CostBasisEngine.Result(List.of(), List.of()));

    var response = service.performance("ALL", "lifo");

    assertEquals("ALL", response.window());
    assertEquals("lifo", response.method());
    assertEquals("USD", response.baseCurrency());
  }

  @Test
  void performanceRejectsInvalidMatchingMethod() {
    var service = service();
    var user = new AppUser();
    user.id = 1L;
    when(currentUser.require()).thenReturn(user);

    var error = assertThrows(ApiException.class, () -> service.performance("1Y", "bad"));

    assertEquals("validation_error", error.code());
  }

  private PerformanceService service() {
    var service = new PerformanceService();
    service.transactionRepository = transactionRepository;
    service.instrumentRepository = instrumentRepository;
    service.currentUser = currentUser;
    service.lotMatchingService = lotMatchingService;
    service.currencyService = currencyService;
    service.historicalBackfillService = historicalBackfillService;
    service.fxHistoricalBackfillService = fxHistoricalBackfillService;
    return service;
  }

  private PortfolioTransaction tx(
      String symbol, String type, String date, String quantity, String price, String amount, String currency) {
    var tx = new PortfolioTransaction();
    tx.instrumentSymbol = symbol;
    tx.transactionType = type;
    tx.tradeDate = LocalDate.parse(date);
    tx.quantity = new BigDecimal(quantity);
    tx.price = new BigDecimal(price);
    tx.amount = amount == null ? null : new BigDecimal(amount);
    tx.fees = BigDecimal.ZERO;
    tx.currency = currency;
    return tx;
  }

  private Instrument instrument(String symbol, String currency) {
    var instrument = new Instrument();
    instrument.symbol = symbol;
    instrument.currency = currency;
    return instrument;
  }

  private InstrumentPriceBar bar(String symbol, String date, String close) {
    var bar = new InstrumentPriceBar();
    bar.instrumentSymbol = symbol;
    bar.tradeDate = LocalDate.parse(date);
    bar.closePrice = new BigDecimal(close);
    return bar;
  }
}
