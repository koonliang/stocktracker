package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.persistence.InstrumentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CashBalanceServiceTest {
  private final InstrumentRepository instruments = Mockito.mock(InstrumentRepository.class);
  private final CurrencyService currencyService = Mockito.mock(CurrencyService.class);
  private CashBalanceService service;

  @BeforeEach
  void setUp() {
    service = new CashBalanceService();
    service.instrumentRepository = instruments;
    service.currencyService = currencyService;
  }

  @Test
  void computesBalancesAcrossCashAndSecurityTransactions() {
    when(instruments.findBySymbol("AAPL")).thenReturn(Optional.of(instrument("USD")));

    var balances =
        service.balances(
            List.of(
                transaction("deposit", null, null, null, null, "1000", "USD"),
                transaction("withdrawal", null, null, null, null, "100", "USD"),
                transaction("dividend", "AAPL", null, null, "5", "20", null),
                transaction("buy", "AAPL", "2", "100", "1", null, null),
                transaction("sell", "AAPL", "1", "120", "2", null, null)));

    assertEquals(new BigDecimal("832"), balances.get("USD"));
  }

  @Test
  void convertsTotalAndPropagatesStaleFlag() {
    when(currencyService.convert(new BigDecimal("100"), "USD", "SGD", LocalDate.of(2025, 1, 1)))
        .thenReturn(
            new CurrencyService.Converted(
                new BigDecimal("135"), LocalDate.now(), FxStatus.current));
    when(currencyService.convert(new BigDecimal("50"), "EUR", "SGD", LocalDate.of(2025, 1, 1)))
        .thenReturn(
            new CurrencyService.Converted(new BigDecimal("73"), LocalDate.now(), FxStatus.stale));

    var total =
        service.baseConvertedTotal(
            new java.util.LinkedHashMap<>(
                java.util.Map.of("USD", new BigDecimal("100"), "EUR", new BigDecimal("50"))),
            "SGD",
            LocalDate.of(2025, 1, 1));

    assertEquals(new BigDecimal("208"), total.value());
    assertTrue(total.stale());
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
    tx.transactionType = type;
    tx.instrumentSymbol = symbol;
    tx.quantity = quantity == null ? null : new BigDecimal(quantity);
    tx.price = price == null ? null : new BigDecimal(price);
    tx.fees = fees == null ? null : new BigDecimal(fees);
    tx.amount = amount == null ? null : new BigDecimal(amount);
    tx.currency = currency;
    return tx;
  }

  private Instrument instrument(String currency) {
    var instrument = new Instrument();
    instrument.currency = currency;
    return instrument;
  }
}
