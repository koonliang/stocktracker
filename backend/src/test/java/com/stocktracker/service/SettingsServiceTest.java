package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.security.CurrentUser;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SettingsServiceTest {
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private final CurrencyService currencyService = Mockito.mock(CurrencyService.class);
  private final FxRefreshJob fxRefreshJob = Mockito.mock(FxRefreshJob.class);
  private SettingsService service;

  @BeforeEach
  void setUp() {
    service = new SettingsService();
    service.currentUser = currentUser;
    service.currencyService = currencyService;
    service.fxRefreshJob = fxRefreshJob;
    service.defaultBaseCurrency = "USD";
  }

  @Test
  void getBaseCurrencyReturnsCurrentUserAndSupportedList() {
    var user = new AppUser();
    user.baseCurrency = "SGD";
    when(currentUser.require()).thenReturn(user);
    when(currencyService.supportedCurrencies("USD"))
        .thenReturn(new TreeSet<>(java.util.List.of("USD", "SGD")));

    var response = service.getBaseCurrency();

    assertEquals("SGD", response.baseCurrency());
    assertEquals(java.util.List.of("SGD", "USD"), response.supported());
  }

  @Test
  void updateBaseCurrencyRejectsUnsupportedValue() {
    when(currencyService.supportedCurrencies("USD"))
        .thenReturn(new TreeSet<>(java.util.List.of("USD", "SGD")));

    var error = assertThrows(ApiException.class, () -> service.updateBaseCurrency("eur"));

    assertEquals("unsupported_currency", error.code());
  }

  @Test
  void updateBaseCurrencyNormalizesAndRefreshesFx() {
    var user = new AppUser();
    user.baseCurrency = "USD";
    when(currentUser.require()).thenReturn(user);
    when(currencyService.supportedCurrencies("USD"))
        .thenReturn(new TreeSet<>(java.util.List.of("USD", "SGD")));

    var response = service.updateBaseCurrency(" sgd ");

    assertEquals("SGD", user.baseCurrency);
    assertEquals("SGD", response.baseCurrency());
    verify(fxRefreshJob).refresh();
  }
}
