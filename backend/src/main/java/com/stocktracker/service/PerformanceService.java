package com.stocktracker.service;

import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.dto.PerformanceResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@ApplicationScoped
public class PerformanceService {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject CurrentUser currentUser;
  @Inject LotMatchingService lotMatchingService;
  @Inject CurrencyService currencyService;
  @Inject HistoricalBackfillService historicalBackfillService;

  public PerformanceResponse performance(String window, String method) {
    var user = currentUser.require();
    var baseCurrency = user.baseCurrency == null ? "USD" : user.baseCurrency;
    var normalizedWindow = normalizeWindow(window);
    var normalizedMethod = normalizeMethod(method);
    var today = LocalDate.now();
    var start = windowStart(normalizedWindow, today);
    var transactions = transactionRepository.listAscending(user.id);
    var symbols =
        transactions.stream()
            .map(transaction -> transaction.instrumentSymbol)
            .filter(Objects::nonNull)
            .map(String::toUpperCase)
            .collect(Collectors.toCollection(TreeSet::new));

    for (var symbol : symbols) {
      ensureHistory(symbol, start, today);
    }

    var instruments = instrumentRepository.findBySymbols(symbols);
    var barsBySymbol = groupBars(instrumentRepository.listPriceBars(symbols));
    var matched = lotMatchingService.match(transactions, normalizedMethod);

    var closedLots =
        matched.closedLots().stream()
            .map(
                lot -> {
                  var currency = currencyFor(lot.symbol(), instruments, baseCurrency);
                  var base =
                      currencyService.convert(
                          lot.realizedPnl(), currency, baseCurrency, lot.closedOn());
                  return new PerformanceResponse.ClosedLotView(
                      lot.symbol(),
                      currency,
                      lot.openedOn().toString(),
                      lot.closedOn().toString(),
                      dbl(lot.quantity(), 6),
                      dbl(lot.costBasis(), 4),
                      dbl(lot.proceeds(), 4),
                      dbl(lot.realizedPnl(), 4),
                      dbl(base.value(), 4));
                })
            .toList();

    var incomeEvents = dividendIncomeEvents(transactions, instruments, baseCurrency);
    var realized =
        closedLots.stream()
            .map(lot -> BigDecimal.valueOf(lot.realizedPnLBase()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(
                incomeEvents.stream()
                    .map(event -> BigDecimal.valueOf(event.amountBase()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
    var unrealized =
        unrealized(transactions, matched, instruments, barsBySymbol, baseCurrency, today);
    var series =
        returnSeries(transactions, symbols, instruments, barsBySymbol, baseCurrency, start, today);
    var twr =
        series.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(series.get(series.size() - 1).cumulativeReturnPct());
    var contributions =
        contributions(matched, instruments, barsBySymbol, baseCurrency, today, unrealized);

    return new PerformanceResponse(
        normalizedWindow,
        normalizedMethod,
        baseCurrency,
        dbl(realized, 4),
        dbl(unrealized, 4),
        dbl(twr, 4),
        closedLots,
        incomeEvents,
        series,
        contributions);
  }

  private BigDecimal unrealized(
      List<PortfolioTransaction> transactions,
      CostBasisEngine.Result matched,
      Map<String, com.stocktracker.domain.Instrument> instruments,
      Map<String, List<InstrumentPriceBar>> barsBySymbol,
      String baseCurrency,
      LocalDate today) {
    var total = BigDecimal.ZERO;
    for (var lot : matched.openLots()) {
      var currency = currencyFor(lot.symbol(), instruments, baseCurrency);
      var price = closeOnOrBefore(barsBySymbol.getOrDefault(lot.symbol(), List.of()), today);
      var value = lot.quantity().multiply(price).subtract(lot.totalCost());
      total = total.add(currencyService.convert(value, currency, baseCurrency, today).value());
    }
    return total;
  }

  private List<PerformanceResponse.IncomeEventView> dividendIncomeEvents(
      List<PortfolioTransaction> transactions,
      Map<String, com.stocktracker.domain.Instrument> instruments,
      String baseCurrency) {
    var events = new ArrayList<PerformanceResponse.IncomeEventView>();
    for (var transaction : transactions) {
      if (!"dividend".equalsIgnoreCase(transaction.transactionType)) {
        continue;
      }
      var amount = transaction.amount == null ? BigDecimal.ZERO : transaction.amount;
      var fees = transaction.fees == null ? BigDecimal.ZERO : transaction.fees;
      var currency =
          transaction.currency == null || transaction.currency.isBlank()
              ? currencyFor(transaction.instrumentSymbol, instruments, baseCurrency)
              : transaction.currency;
      var netAmount = amount.subtract(fees);
      var base = currencyService.convert(netAmount, currency, baseCurrency, transaction.tradeDate);
      events.add(
          new PerformanceResponse.IncomeEventView(
              transaction.instrumentSymbol,
              currency,
              transaction.tradeDate.toString(),
              transaction.transactionType,
              dbl(netAmount, 4),
              dbl(base.value(), 4)));
    }
    return events;
  }

  private List<PerformanceResponse.ReturnPoint> returnSeries(
      List<PortfolioTransaction> transactions,
      Set<String> symbols,
      Map<String, com.stocktracker.domain.Instrument> instruments,
      Map<String, List<InstrumentPriceBar>> barsBySymbol,
      String baseCurrency,
      LocalDate start,
      LocalDate today) {
    if (symbols.isEmpty()) {
      return List.of(
          new PerformanceResponse.ReturnPoint(start.toString(), 0),
          new PerformanceResponse.ReturnPoint(today.toString(), 0));
    }
    var dates = new TreeSet<LocalDate>();
    dates.add(start);
    dates.add(today);
    barsBySymbol
        .values()
        .forEach(bars -> bars.stream().map(bar -> bar.tradeDate).forEach(dates::add));

    BigDecimal previousValue = null;
    var cumulativeFactor = BigDecimal.ONE;
    var out = new ArrayList<PerformanceResponse.ReturnPoint>();
    for (var date : dates) {
      if (date.isBefore(start) || date.isAfter(today)) {
        continue;
      }
      var value =
          portfolioValue(transactions, symbols, instruments, barsBySymbol, baseCurrency, date);
      if (previousValue != null && previousValue.compareTo(BigDecimal.ZERO) > 0) {
        cumulativeFactor = nextTwrFactor(cumulativeFactor, previousValue, value);
      }
      out.add(
          new PerformanceResponse.ReturnPoint(
              date.toString(), dbl(factorToPercent(cumulativeFactor), 4)));
      previousValue = value;
    }
    return out;
  }

  private BigDecimal portfolioValue(
      List<PortfolioTransaction> transactions,
      Set<String> symbols,
      Map<String, com.stocktracker.domain.Instrument> instruments,
      Map<String, List<InstrumentPriceBar>> barsBySymbol,
      String baseCurrency,
      LocalDate date) {
    var throughDate = transactions.stream().filter(tx -> !tx.tradeDate.isAfter(date)).toList();
    var snapshot = lotMatchingService.match(throughDate, "fifo");
    var total = BigDecimal.ZERO;
    for (var symbol : symbols) {
      var shares = snapshot.shares(symbol);
      if (shares.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      var currency = currencyFor(symbol, instruments, baseCurrency);
      var value =
          shares.multiply(closeOnOrBefore(barsBySymbol.getOrDefault(symbol, List.of()), date));
      total = total.add(currencyService.convert(value, currency, baseCurrency, date).value());
    }
    return total;
  }

  private List<PerformanceResponse.ContributionView> contributions(
      CostBasisEngine.Result matched,
      Map<String, com.stocktracker.domain.Instrument> instruments,
      Map<String, List<InstrumentPriceBar>> barsBySymbol,
      String baseCurrency,
      LocalDate today,
      BigDecimal totalUnrealized) {
    var bySymbol = new LinkedHashMap<String, BigDecimal>();
    for (var lot : matched.openLots()) {
      var currency = currencyFor(lot.symbol(), instruments, baseCurrency);
      var pnl =
          lot.quantity()
              .multiply(closeOnOrBefore(barsBySymbol.getOrDefault(lot.symbol(), List.of()), today))
              .subtract(lot.totalCost());
      bySymbol.merge(
          lot.symbol(),
          currencyService.convert(pnl, currency, baseCurrency, today).value(),
          BigDecimal::add);
    }
    return bySymbol.entrySet().stream()
        .map(
            entry ->
                new PerformanceResponse.ContributionView(
                    entry.getKey(), dbl(contributionPct(entry.getValue(), totalUnrealized), 4)))
        .toList();
  }

  static BigDecimal nextTwrFactor(
      BigDecimal cumulativeFactor, BigDecimal previousValue, BigDecimal value) {
    if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
      return cumulativeFactor;
    }
    var periodReturn = value.subtract(previousValue).divide(previousValue, 8, RoundingMode.HALF_UP);
    return cumulativeFactor.multiply(BigDecimal.ONE.add(periodReturn));
  }

  static BigDecimal factorToPercent(BigDecimal factor) {
    return factor.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100));
  }

  static BigDecimal contributionPct(BigDecimal contribution, BigDecimal total) {
    if (total.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return contribution.divide(total, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  private void ensureHistory(String symbol, LocalDate start, LocalDate today) {
    var bars = instrumentRepository.listPriceBars(symbol);
    if (bars.isEmpty()
        || bars.get(0).tradeDate.isAfter(start)
        || bars.get(bars.size() - 1).tradeDate.isBefore(today.minusDays(1))) {
      historicalBackfillService.backfill(symbol, start);
    }
  }

  private Map<String, List<InstrumentPriceBar>> groupBars(List<InstrumentPriceBar> bars) {
    var grouped = new HashMap<String, List<InstrumentPriceBar>>();
    for (var bar : bars) {
      grouped.computeIfAbsent(bar.instrumentSymbol, ignored -> new ArrayList<>()).add(bar);
    }
    grouped.values().forEach(list -> list.sort(Comparator.comparing(bar -> bar.tradeDate)));
    return grouped;
  }

  private BigDecimal closeOnOrBefore(List<InstrumentPriceBar> bars, LocalDate date) {
    BigDecimal last = BigDecimal.ZERO;
    for (var bar : bars) {
      if (bar.tradeDate.isAfter(date)) {
        break;
      }
      last = bar.closePrice;
    }
    return last;
  }

  private String currencyFor(
      String symbol, Map<String, com.stocktracker.domain.Instrument> instruments, String fallback) {
    var instrument = instruments.get(symbol);
    return instrument == null || instrument.currency == null ? fallback : instrument.currency;
  }

  private String normalizeWindow(String window) {
    var value = window == null || window.isBlank() ? "1Y" : window.toUpperCase(Locale.ROOT);
    return switch (value) {
      case "1M", "3M", "6M", "1Y", "YTD", "ALL" -> value;
      default -> "1Y";
    };
  }

  private String normalizeMethod(String method) {
    return CostBasisEngine.MatchingMethod.parse(method).name().toLowerCase(Locale.ROOT);
  }

  private LocalDate windowStart(String window, LocalDate today) {
    return switch (window) {
      case "1M" -> today.minusMonths(1);
      case "3M" -> today.minusMonths(3);
      case "6M" -> today.minusMonths(6);
      case "YTD" -> LocalDate.of(today.getYear(), 1, 1);
      case "ALL" -> LocalDate.of(1970, 1, 1);
      default -> today.minusYears(1);
    };
  }

  private static double dbl(BigDecimal value, int scale) {
    return value.setScale(scale, RoundingMode.HALF_UP).doubleValue();
  }
}
