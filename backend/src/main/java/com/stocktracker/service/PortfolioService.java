package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.dto.ConversionDtos.ConversionMetadata;
import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.dto.TransactionRequest;
import com.stocktracker.dto.TransactionResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PortfolioService {
  private static final Set<String> SECURITY_TYPES = Set.of("buy", "sell", "dividend", "split");
  private static final Set<String> CASH_TYPES = Set.of("deposit", "withdrawal", "fee");

  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject TransactionValidationService transactionValidationService;
  @Inject CurrentUser currentUser;
  @Inject QuoteCacheService quoteCacheService;
  @Inject CurrencyService currencyService;
  @Inject CostBasisEngine costBasisEngine;
  @Inject TransactionCurrencyBackfillService transactionCurrencyBackfillService;
  @Inject OnDemandFxService onDemandFxService;
  @Inject PortfolioService self;

  @org.eclipse.microprofile.config.inject.ConfigProperty(
      name = "stocktracker.base-currency.default",
      defaultValue = "USD")
  String defaultBaseCurrency;

  public DashboardResponse getDashboard() {
    transactionCurrencyBackfillService.backfillCurrentUser();
    return buildDashboard(transactionRepository.listAscending(currentUser.id()));
  }

  public List<TransactionResponse> listTransactions() {
    transactionCurrencyBackfillService.backfillCurrentUser();
    return transactionRepository.listDescending(currentUser.id()).stream()
        .map(this::toResponse)
        .toList();
  }

  public Map<String, BigDecimal> currentShareBalances() {
    var balances = new LinkedHashMap<String, BigDecimal>();
    for (var transaction : transactionRepository.listAscending(currentUser.id())) {
      var ticker = transaction.instrumentSymbol;
      if (ticker == null) {
        continue; // cash events carry no symbol
      }
      var quantity = transaction.quantity;
      var current = balances.getOrDefault(ticker, BigDecimal.ZERO);
      switch (transaction.transactionType) {
        case "buy" -> balances.put(ticker, current.add(quantity));
        case "sell" -> balances.put(ticker, current.subtract(quantity));
        case "split" -> balances.put(ticker, current.multiply(quantity));
        default -> {
          // dividend / cash types do not change the share balance
        }
      }
    }
    return balances;
  }

  public void createTransactions(List<TransactionRequest> requests, String source) {
    var normalized = requests.stream().map(transactionValidationService::normalize).toList();
    preflightHistoricalFx(normalized);
    self.createTransactionsTransactional(normalized, source);
  }

  @Transactional
  void createTransactionsTransactional(List<TransactionRequest> normalized, String source) {
    transactionValidationService.validateBatch(normalized, currentShareBalances());
    var userId = currentUser.id();
    for (var request : normalized) {
      var transaction = new PortfolioTransaction();
      transaction.userId = userId;
      transaction.tradeDate = request.date();
      transaction.instrumentSymbol = request.ticker();
      transaction.transactionType = request.type();
      transaction.quantity = request.quantity() == null ? BigDecimal.ZERO : request.quantity();
      transaction.price = request.price() == null ? BigDecimal.ZERO : request.price();
      transaction.fees = request.fees() == null ? BigDecimal.ZERO : request.fees();
      transaction.amount = request.amount();
      transaction.currency = request.currency();
      if (transaction.currency == null) {
        transactionCurrencyBackfillService.backfill(
            transaction,
            currentUser.optional().map(user -> user.baseCurrency).orElse(defaultBaseCurrency));
      } else {
        transaction.currencySource = "provided";
      }
      transaction.source = source;
      transactionRepository.persist(transaction);
    }
  }

  void preflightHistoricalFx(List<TransactionRequest> requests) {
    var baseCurrency =
        currentUser
            .optional()
            .map(user -> user.baseCurrency)
            .filter(currency -> currency != null && !currency.isBlank())
            .orElse(defaultBaseCurrency)
            .toUpperCase();
    for (var request : requests) {
      var fromCurrency = fxSourceCurrency(request);
      if (fromCurrency == null || request.date() == null) {
        continue;
      }
      onDemandFxService.ensureRate(fromCurrency, baseCurrency, request.date());
    }
  }

  String fxSourceCurrency(TransactionRequest request) {
    if (request.type() == null) {
      return null;
    }
    if (SECURITY_TYPES.contains(request.type())) {
      var instrument = instrumentRepository.findBySymbol(request.ticker()).orElse(null);
      return instrument == null ? null : instrument.currency;
    }
    if (CASH_TYPES.contains(request.type())) {
      return request.currency();
    }
    return null;
  }

  @Transactional
  public DashboardResponse deleteTransaction(Long transactionId) {
    var transaction =
        transactionRepository
            .findByIdAndUser(transactionId, currentUser.id())
            .orElseThrow(
                () ->
                    new ApiException(Status.NOT_FOUND, "not_found", "Transaction does not exist"));
    transactionRepository.delete(transaction);
    return getDashboard();
  }

  public DashboardResponse buildDashboard(List<PortfolioTransaction> transactions) {
    var baseCurrency =
        currentUser.optional().map(user -> user.baseCurrency).orElse(defaultBaseCurrency);
    var today = LocalDate.now();

    var symbols =
        transactions.stream()
            .map(transaction -> transaction.instrumentSymbol)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (symbols.isEmpty()) {
      return new DashboardResponse(
          new DashboardResponse.Summary(
              0,
              0,
              0,
              0,
              0,
              0,
              baseCurrency,
              conversion(baseCurrency, 0, today, "current"),
              conversion(baseCurrency, 0, today, "current"),
              conversion(baseCurrency, 0, today, "current")),
          List.of(),
          List.of());
    }

    var instruments = instrumentRepository.findBySymbols(symbols);
    var barsBySymbol = groupBars(instrumentRepository.listPriceBars(symbols));
    var quotes = quoteCacheService.cachedBySymbol(symbols);
    var costBasis = costBasisEngine.replay(transactions);

    List<DashboardResponse.Holding> holdings = new ArrayList<>();
    var totalMarketValue = BigDecimal.ZERO;
    var totalCostBasis = BigDecimal.ZERO;
    var totalDayChange = BigDecimal.ZERO;
    var warnings = new ArrayList<String>();
    var summaryFxDate = today;
    var summaryFxStale = false;
    var summaryFxUnavailable = false;

    for (var symbol : symbols) {
      var shares = costBasis.shares(symbol);
      if (shares.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      var instrument = instruments.get(symbol);
      var nativeCurrency = instrument == null ? baseCurrency : instrument.currency;
      var bars = barsBySymbol.getOrDefault(symbol, List.of());
      var price = currentPrice(quotes.get(symbol), bars);

      var averageCost = costBasis.averageCost(symbol);
      var nativeCostBasis = costBasis.costBasis(symbol);
      var nativeMarketValue = shares.multiply(price.price());
      var nativeDayChange = shares.multiply(price.price().subtract(price.previousClose()));

      var baseCostBasis =
          currencyService.convertHolding(nativeCostBasis, nativeCurrency, baseCurrency, today);
      var baseMarketValue =
          currencyService.convertHolding(nativeMarketValue, nativeCurrency, baseCurrency, today);
      var basePrice =
          currencyService.convertHolding(price.price(), nativeCurrency, baseCurrency, today);
      var baseDayChange =
          currencyService.convertHolding(nativeDayChange, nativeCurrency, baseCurrency, today);
      var baseUnrealized = baseMarketValue.value().subtract(baseCostBasis.value());

      if (baseMarketValue.unavailable()
          || baseCostBasis.unavailable()
          || baseDayChange.unavailable()) {
        warnings.add(
            "FX rate unavailable for " + symbol + " " + nativeCurrency + " to " + baseCurrency);
        summaryFxUnavailable = true;
      } else {
        totalMarketValue = totalMarketValue.add(baseMarketValue.value());
        totalCostBasis = totalCostBasis.add(baseCostBasis.value());
        totalDayChange = totalDayChange.add(baseDayChange.value());
        summaryFxStale =
            summaryFxStale
                || baseMarketValue.stale()
                || baseCostBasis.stale()
                || baseDayChange.stale();
        summaryFxDate = oldest(summaryFxDate, baseMarketValue.fxDate());
        summaryFxDate = oldest(summaryFxDate, baseCostBasis.fxDate());
        summaryFxDate = oldest(summaryFxDate, baseDayChange.fxDate());
      }

      var fxStale =
          baseMarketValue.stale()
              || baseCostBasis.stale()
              || baseDayChange.stale()
              || baseMarketValue.unavailable()
              || baseCostBasis.unavailable()
              || baseDayChange.unavailable();

      holdings.add(
          new DashboardResponse.Holding(
              symbol,
              instrument == null ? symbol : instrument.name,
              scale6(shares),
              nativeCurrency,
              scale4(averageCost),
              scale4(price.price()),
              scale4(nativeMarketValue),
              scale4(baseCostBasis.value()),
              scale4(basePrice.value()),
              scale4(baseMarketValue.value()),
              scale4(baseUnrealized),
              ratio(baseUnrealized, baseCostBasis.value()),
              scale4(baseDayChange.value()),
              ratio(price.price().subtract(price.previousClose()), price.previousClose()),
              0,
              baseCurrency,
              conversion(baseCurrency, baseCostBasis),
              conversion(baseCurrency, basePrice),
              conversion(baseCurrency, baseMarketValue),
              conversion(baseCurrency, baseDayChange),
              price.asOf(),
              price.fetchedAt(),
              price.stale() || fxStale));
    }

    var portfolioMarketValue = totalMarketValue;
    holdings =
        holdings.stream()
            .sorted((left, right) -> Double.compare(right.marketValue(), left.marketValue()))
            .map(holding -> withWeight(holding, portfolioMarketValue))
            .toList();

    var totalUnrealizedPnL = totalMarketValue.subtract(totalCostBasis);
    var previousPortfolioValue = totalMarketValue.subtract(totalDayChange);

    return new DashboardResponse(
        new DashboardResponse.Summary(
            scale4(totalMarketValue),
            scale4(totalCostBasis),
            scale4(totalUnrealizedPnL),
            ratio(totalUnrealizedPnL, totalCostBasis),
            scale4(totalDayChange),
            ratio(totalDayChange, previousPortfolioValue),
            baseCurrency,
            conversion(
                baseCurrency,
                totalMarketValue,
                summaryFxDate,
                summaryStatus(summaryFxUnavailable, summaryFxStale)),
            conversion(
                baseCurrency,
                totalCostBasis,
                summaryFxDate,
                summaryStatus(summaryFxUnavailable, summaryFxStale)),
            conversion(
                baseCurrency,
                totalDayChange,
                summaryFxDate,
                summaryStatus(summaryFxUnavailable, summaryFxStale))),
        holdings,
        warnings);
  }

  private DashboardResponse.Holding withWeight(
      DashboardResponse.Holding holding, BigDecimal portfolioMarketValue) {
    var weight =
        portfolioMarketValue.compareTo(BigDecimal.ZERO) > 0
            ? scale4(
                BigDecimal.valueOf(holding.marketValue())
                    .divide(portfolioMarketValue, 8, RoundingMode.HALF_UP))
            : 0;
    return new DashboardResponse.Holding(
        holding.ticker(),
        holding.name(),
        holding.shares(),
        holding.currency(),
        holding.averageCost(),
        holding.nativePrice(),
        holding.nativeMarketValue(),
        holding.costBasis(),
        holding.currentPrice(),
        holding.marketValue(),
        holding.unrealizedPnL(),
        holding.unrealizedPnLPct(),
        holding.dayChange(),
        holding.dayChangePct(),
        weight,
        holding.baseCurrency(),
        holding.costBasisConversion(),
        holding.priceConversion(),
        holding.marketValueConversion(),
        holding.dayChangeConversion(),
        holding.asOf(),
        holding.fetchedAt(),
        holding.stale());
  }

  private record CurrentPrice(
      BigDecimal price,
      BigDecimal previousClose,
      java.time.Instant asOf,
      java.time.Instant fetchedAt,
      boolean stale) {}

  /** Native current price for a symbol: live quote if present, else latest price bar (stale). */
  private CurrentPrice currentPrice(InstrumentQuote quote, List<InstrumentPriceBar> bars) {
    if (quote != null && quote.price != null) {
      var previous =
          quote.previousClose != null ? quote.previousClose : previousClose(bars, quote.price);
      return new CurrentPrice(
          quote.price,
          previous,
          quote.asOf,
          quote.fetchedAt,
          quoteCacheService.effectiveStale(quote));
    }
    var current = latestClose(bars);
    return new CurrentPrice(current, previousClose(bars, current), null, null, true);
  }

  public PositionSnapshot findPosition(String symbol) {
    var user = currentUser.optional().orElse(null);
    if (user == null) {
      return null;
    }
    var dashboard = buildDashboard(transactionRepository.listAscending(user.id));
    return dashboard.holdings().stream()
        .filter(holding -> holding.ticker().equalsIgnoreCase(symbol))
        .findFirst()
        .map(
            holding ->
                new PositionSnapshot(
                    holding.shares(),
                    holding.averageCost(),
                    holding.marketValue(),
                    holding.unrealizedPnL(),
                    holding.unrealizedPnLPct()))
        .orElse(null);
  }

  private TransactionResponse toResponse(PortfolioTransaction transaction) {
    return new TransactionResponse(
        transaction.id.toString(),
        transaction.tradeDate.toString(),
        transaction.instrumentSymbol,
        transaction.transactionType,
        scale6(transaction.quantity),
        scale4(transaction.price),
        scale4(transaction.fees),
        transaction.amount == null ? null : scale4(transaction.amount),
        transaction.currency,
        transaction.currencySource,
        transaction.source);
  }

  private Map<String, List<InstrumentPriceBar>> groupBars(List<InstrumentPriceBar> bars) {
    var grouped = new HashMap<String, List<InstrumentPriceBar>>();
    for (var bar : bars) {
      grouped.computeIfAbsent(bar.instrumentSymbol, ignored -> new ArrayList<>()).add(bar);
    }
    return grouped;
  }

  private BigDecimal latestClose(List<InstrumentPriceBar> bars) {
    if (bars.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return bars.get(bars.size() - 1).closePrice;
  }

  private BigDecimal previousClose(List<InstrumentPriceBar> bars, BigDecimal fallback) {
    if (bars.size() < 2) {
      return fallback;
    }
    return bars.get(bars.size() - 2).closePrice;
  }

  private static double ratio(BigDecimal numerator, BigDecimal denominator) {
    if (denominator.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }
    return numerator.divide(denominator, 8, RoundingMode.HALF_UP).doubleValue();
  }

  private static double scale4(BigDecimal value) {
    return value.setScale(4, RoundingMode.HALF_UP).doubleValue();
  }

  private static double scale6(BigDecimal value) {
    return value.setScale(6, RoundingMode.HALF_UP).doubleValue();
  }

  private static ConversionMetadata conversion(
      String baseCurrency, CurrencyService.Converted converted) {
    return new ConversionMetadata(
        baseCurrency, scale4(converted.value()), converted.fxDate(), converted.fxStatus());
  }

  private static ConversionMetadata conversion(
      String baseCurrency, BigDecimal value, LocalDate fxDate, String status) {
    return new ConversionMetadata(
        baseCurrency,
        scale4(value),
        fxDate,
        com.stocktracker.dto.ConversionDtos.FxStatus.valueOf(status));
  }

  private static ConversionMetadata conversion(
      String baseCurrency, double value, LocalDate fxDate, String status) {
    return conversion(baseCurrency, BigDecimal.valueOf(value), fxDate, status);
  }

  private static LocalDate oldest(LocalDate left, LocalDate right) {
    if (right == null) {
      return left;
    }
    return right.isBefore(left) ? right : left;
  }

  private static String summaryStatus(boolean unavailable, boolean stale) {
    if (unavailable) {
      return "unavailable";
    }
    return stale ? "stale" : "current";
  }

  public record PositionSnapshot(
      double shares,
      double averageCost,
      double marketValue,
      double unrealizedPnL,
      double unrealizedPnLPct) {}
}
