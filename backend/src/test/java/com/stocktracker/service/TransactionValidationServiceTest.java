package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  void cashDepositRequiresCurrency() {
    var issue =
        service.validate(
            new TransactionRequest(
                TODAY, null, "deposit", null, null, null, new BigDecimal("1000"), null),
            Map.of());

    assertEquals("deposit requires a currency", issue);
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
