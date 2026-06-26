package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.Instrument;
import com.stocktracker.dto.TransactionRequest;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionValidationServiceTest {
  private static final LocalDate TODAY = LocalDate.now();

  private final StubInstrumentRepository instrumentRepository = new StubInstrumentRepository();
  private final StubOnDemandFxService onDemandFxService = new StubOnDemandFxService();

  private TransactionValidationService service;

  @BeforeEach
  void setUp() {
    service = new TransactionValidationService();
    service.instrumentRepository = instrumentRepository;
    service.onDemandFxService = onDemandFxService;
    service.currentUser = new StubCurrentUser(null);
    service.defaultBaseCurrency = "USD";

    instrumentRepository.addInstrument("AAPL", "USD");
  }

  @Test
  void normalizeTrimsAndUppercasesFields() {
    var normalized =
        service.normalize(
            new TransactionRequest(
                TODAY,
                " aapl ",
                " Buy ",
                new BigDecimal("10.0"),
                new BigDecimal("100.0000"),
                new BigDecimal("1.5000"),
                new BigDecimal("1000.0000"),
                " sgd "));

    assertEquals("AAPL", normalized.ticker());
    assertEquals("buy", normalized.type());
    assertEquals(0, normalized.quantity().compareTo(new BigDecimal("10")));
    assertEquals(0, normalized.price().compareTo(new BigDecimal("100")));
    assertEquals(0, normalized.fees().compareTo(new BigDecimal("1.5")));
    assertEquals(0, normalized.amount().compareTo(new BigDecimal("1000")));
    assertEquals("SGD", normalized.currency());
  }

  @Test
  void normalizeHandlesBlankAndNullValues() {
    var normalized =
        service.normalize(
            new TransactionRequest(
                TODAY,
                " ",
                null,
                null,
                null,
                null,
                null,
                " "));

    assertNull(normalized.ticker());
    assertNull(normalized.type());
    assertNull(normalized.quantity());
    assertNull(normalized.price());
    assertEquals(0, normalized.fees().compareTo(BigDecimal.ZERO));
    assertNull(normalized.amount());
    assertNull(normalized.currency());
  }

  @Test
  void securityBuyRequiresInstrumentCurrency() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "AAPL",
                "buy",
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "SGD"),
            Map.of());

    assertEquals("currency must match the instrument currency (USD)", issue);
  }

  @Test
  void securityBuyWithNullCurrencyDefaultsToInstrument() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "AAPL",
                "buy",
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                null),
            Map.of());

    assertNull(issue);
  }

  @Test
  void securityBuyWithMatchingCurrencyAndNoFxRequirementPasses() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "AAPL",
                "buy",
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "USD"),
            Map.of());

    assertNull(issue);
  }

  @Test
  void cashDepositRequiresCurrency() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY, null, "deposit", null, null, null, new BigDecimal("1000"), null),
            Map.of());

    assertEquals("deposit requires a currency", issue);
  }

  @Test
  void cashWithdrawalAndFeeValidatePositiveAmount() {
    assertNull(
        service.validate(
            new TransactionRequest(
                TODAY, null, "withdrawal", null, null, null, new BigDecimal("100"), "USD"),
            Map.of()));
    assertNull(
        service.validate(
            new TransactionRequest(TODAY, null, "fee", null, null, null, new BigDecimal("1"), "USD"),
            Map.of()));
  }

  @Test
  void unsupportedCurrencyIsRejectedForCashTypes() {
    onDemandFxService.markUnavailable("XYZ", "USD");

    var issue =
        service.validate(
            new TransactionRequest(
                TODAY, null, "deposit", null, null, null, new BigDecimal("1000"), "XYZ"),
            Map.of());

    assertEquals("FX rate unavailable for XYZ to USD", issue);
  }

  @Test
  void sellQuantityCannotExceedHeldShares() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "AAPL",
                "sell",
                new BigDecimal("5"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "USD"),
            Map.of("AAPL", new BigDecimal("3")));

    assertEquals("sell quantity exceeds held shares", issue);
  }

  @Test
  void sellQuantityEqualToHeldSharesIsAllowed() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "AAPL",
                "sell",
                new BigDecimal("3"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "USD"),
            Map.of("AAPL", new BigDecimal("3")));

    assertNull(issue);
  }

  @Test
  void currentUserBaseCurrencyOverridesDefaultForFxValidation() {
    service.currentUser = new StubCurrentUser(userWithBaseCurrency("SGD"));
    onDemandFxService.markUnavailable("JPY", "SGD");
    instrumentRepository.addInstrument("7203.T", "JPY");

    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "7203.T",
                "buy",
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "JPY"),
            Map.of());

    assertEquals("FX rate unavailable for JPY to SGD", issue);
  }

  @Test
  void currentUserBlankBaseCurrencyFallsBackToDefault() {
    var user = new AppUser();
    user.baseCurrency = " ";
    service.currentUser = new StubCurrentUser(user);
    instrumentRepository.addInstrument("7203.T", "JPY");

    var issue =
        service.validate(
            new TransactionRequest(
                TODAY,
                "7203.T",
                "buy",
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "JPY"),
            Map.of());

    assertNull(issue);
  }

  @Test
  void rejectsFutureDateAndUnknownType() {
    assertEquals(
        "date is in the future",
        service.validate(
            new TransactionRequest(
                TODAY.plusDays(1),
                "AAPL",
                "buy",
                new BigDecimal("1"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                null,
                "USD"),
            Map.of()));

    assertEquals(
        "unknown transaction type: weird",
        service.validate(
            new TransactionRequest(TODAY, null, "weird", null, null, null, null, null), Map.of()));
  }

  @Test
  void rejectsSecurityWithoutTickerAndUnknownTicker() {
    assertEquals(
        "buy requires a ticker",
        service.validate(
            new TransactionRequest(
                TODAY, null, "buy", new BigDecimal("1"), new BigDecimal("10"), BigDecimal.ZERO, null, null),
            Map.of()));

    assertEquals(
        "unknown ticker: MISS",
        service.validate(
            new TransactionRequest(
                TODAY, "MISS", "buy", new BigDecimal("1"), new BigDecimal("10"), BigDecimal.ZERO, null, "USD"),
            Map.of()));
  }

  @Test
  void rejectsCashTypesWithTickerAndInvalidTradeFields() {
    assertEquals(
        "deposit must not reference a ticker",
        service.validate(
            new TransactionRequest(
                TODAY, "AAPL", "deposit", null, null, null, new BigDecimal("10"), "USD"),
            Map.of()));

    assertEquals(
        "quantity must be > 0",
        service.validate(
            new TransactionRequest(TODAY, "AAPL", "buy", BigDecimal.ZERO, new BigDecimal("10"), BigDecimal.ZERO, null, "USD"),
            Map.of()));
    assertEquals(
        "price must be > 0",
        service.validate(
            new TransactionRequest(TODAY, "AAPL", "buy", new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO, null, "USD"),
            Map.of()));
    assertEquals(
        "fees must be >= 0",
        service.validate(
            new TransactionRequest(TODAY, "AAPL", "buy", new BigDecimal("1"), new BigDecimal("10"), new BigDecimal("-1"), null, "USD"),
            Map.of()));
  }

  @Test
  void validatesPositiveAmountAndSplitRatio() {
    assertEquals(
        "split ratio must be > 0",
        service.validate(
            new TransactionRequest(TODAY, "AAPL", "split", BigDecimal.ZERO, null, BigDecimal.ZERO, null, "USD"),
            Map.of()));
    assertEquals(
        "amount must be > 0",
        service.validate(
            new TransactionRequest(TODAY, "AAPL", "dividend", null, null, BigDecimal.ZERO, BigDecimal.ZERO, "USD"),
            Map.of()));
  }

  @Test
  void validateBatchAppliesBalancesAndThrowsOnFirstInvalidRequest() {
    var balances = new java.util.LinkedHashMap<String, BigDecimal>();
    balances.put("AAPL", new BigDecimal("2"));

    service.validateBatch(
        java.util.List.of(
            new TransactionRequest(TODAY, "AAPL", "buy", new BigDecimal("1"), new BigDecimal("10"), BigDecimal.ZERO, null, "USD"),
            new TransactionRequest(TODAY, "AAPL", "split", new BigDecimal("2"), null, BigDecimal.ZERO, null, "USD")),
        balances);

    assertEquals(0, balances.get("AAPL").compareTo(new BigDecimal("6")));

    var error =
        assertThrows(
            ApiException.class,
            () ->
                service.validateBatch(
                    java.util.List.of(
                        new TransactionRequest(TODAY, "AAPL", "sell", new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ZERO, null, "USD")),
                    new java.util.LinkedHashMap<>(java.util.Map.of("AAPL", new BigDecimal("1")))));

    assertEquals("validation_error", error.code());
  }

  @Test
  void validateBatchHandlesEmptyInputAndApplyToBalancesSkipsNullTicker() {
    var balances = new java.util.LinkedHashMap<String, BigDecimal>();
    service.validateBatch(java.util.List.of(), balances);
    assertEquals(0, balances.size());

    service.applyToBalances(
        new TransactionRequest(TODAY, null, "buy", new BigDecimal("1"), new BigDecimal("10"), BigDecimal.ZERO, null, "USD"),
        balances);
    assertEquals(0, balances.size());
  }

  @Test
  void applyToBalancesLeavesCashAndDividendUnchangedAndExposesFieldDetail() {
    var balances = new java.util.LinkedHashMap<String, BigDecimal>();
    balances.put("AAPL", new BigDecimal("3"));

    service.applyToBalances(
        new TransactionRequest(TODAY, "AAPL", "dividend", null, null, BigDecimal.ZERO, new BigDecimal("5"), "USD"),
        balances);
    service.applyToBalances(
        new TransactionRequest(TODAY, null, "deposit", null, null, BigDecimal.ZERO, new BigDecimal("5"), "USD"),
        balances);

    assertEquals(0, balances.get("AAPL").compareTo(new BigDecimal("3")));
    assertEquals(java.util.Map.of("field", "ticker", "value", "AAPL"), service.fieldDetail("ticker", "AAPL"));
  }

  private AppUser userWithBaseCurrency(String baseCurrency) {
    var user = new AppUser();
    user.baseCurrency = baseCurrency;
    return user;
  }

  private static final class StubCurrentUser extends CurrentUser {
    private final AppUser user;

    private StubCurrentUser(AppUser user) {
      this.user = user;
    }

    @Override
    public Optional<AppUser> optional() {
      return Optional.ofNullable(user);
    }
  }

  private static final class StubOnDemandFxService extends OnDemandFxService {
    private final java.util.Set<String> unavailablePairs = new java.util.HashSet<>();

    @Override
    public boolean ensureRate(String fromCurrency, String baseCurrency, LocalDate date) {
      return !unavailablePairs.contains(fromCurrency + "->" + baseCurrency);
    }

    private void markUnavailable(String fromCurrency, String baseCurrency) {
      unavailablePairs.add(fromCurrency + "->" + baseCurrency);
    }
  }

  private static final class StubInstrumentRepository extends InstrumentRepository {
    private final java.util.Map<String, Instrument> instruments = new java.util.HashMap<>();

    @Override
    public Optional<Instrument> findBySymbol(String symbol) {
      return Optional.ofNullable(instruments.get(symbol.toUpperCase()));
    }

    private void addInstrument(String symbol, String currency) {
      var instrument = new Instrument();
      instrument.symbol = symbol.toUpperCase();
      instrument.currency = currency;
      instruments.put(instrument.symbol, instrument);
    }
  }
}
