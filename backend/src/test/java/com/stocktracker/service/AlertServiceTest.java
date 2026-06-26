package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.domain.Alert;
import com.stocktracker.dto.AlertDtos.AlertRequest;
import com.stocktracker.persistence.AlertRepository;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.NotificationRepository;
import com.stocktracker.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AlertServiceTest {
  private final AlertRepository alerts = Mockito.mock(AlertRepository.class);
  private final InstrumentRepository instruments = Mockito.mock(InstrumentRepository.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private final NotificationRepository notifications = Mockito.mock(NotificationRepository.class);
  private final MarketDataService marketDataService = Mockito.mock(MarketDataService.class);
  private AlertService service;

  @BeforeEach
  void setUp() {
    service = new AlertService();
    service.alerts = alerts;
    service.instruments = instruments;
    service.currentUser = currentUser;
    service.notifications = notifications;
    service.marketDataService = marketDataService;
  }

  @Test
  void listMapsAlertViews() {
    when(currentUser.id()).thenReturn(1L);
    when(alerts.listForUser(1L)).thenReturn(List.of(alert(4L)));

    var response = service.list();

    assertEquals(1, response.alerts().size());
    assertEquals("4", response.alerts().getFirst().id());
  }

  @Test
  void createPersistsArmedAlert() {
    when(currentUser.id()).thenReturn(1L);
    when(instruments.existsSymbol("AAPL")).thenReturn(true);
    doAnswer(
            invocation -> {
              var alert = invocation.<Alert>getArgument(0);
              alert.id = 6L;
              return null;
            })
        .when(alerts)
        .persist(any(Alert.class));

    var view = service.create(new AlertRequest(" aapl ", " price_above ", new BigDecimal("10")));

    verify(alerts).persist(any(Alert.class));
    assertEquals("6", view.id());
    assertEquals("AAPL", view.symbol());
    assertEquals(true, view.armed());
  }

  @Test
  void createRejectsInvalidCondition() {
    when(currentUser.id()).thenReturn(1L);
    when(instruments.existsSymbol("AAPL")).thenReturn(true);

    var error =
        assertThrows(
            ApiException.class,
            () -> service.create(new AlertRequest("AAPL", "weird", new BigDecimal("10"))));

    assertEquals("validation_error", error.code());
  }

  @Test
  void createTranslatesUnknownSymbolProviderFailure() {
    when(currentUser.id()).thenReturn(1L);
    when(instruments.existsSymbol("MISS")).thenReturn(false);
    when(marketDataService.addInstrument("MISS"))
        .thenThrow(new ApiException(ApiStatuses.UNPROCESSABLE_ENTITY, "unknown_symbol", "nope"));

    var error =
        assertThrows(
            ApiException.class,
            () -> service.create(new AlertRequest("MISS", "price_above", new BigDecimal("10"))));

    assertEquals("validation_error", error.code());
  }

  @Test
  void updateRearmsExistingAlert() {
    when(currentUser.id()).thenReturn(1L);
    var alert = alert(9L);
    alert.armed = false;
    when(alerts.findByIdAndUser(9L, 1L)).thenReturn(Optional.of(alert));
    when(instruments.existsSymbol("AAPL")).thenReturn(true);

    var view = service.update(9L, new AlertRequest("AAPL", "price_below", new BigDecimal("8")));

    assertEquals(true, view.armed());
    verify(alerts).persist(alert);
  }

  @Test
  void deleteRemovesNotificationsThenAlert() {
    when(currentUser.id()).thenReturn(1L);
    var alert = alert(5L);
    when(alerts.findByIdAndUser(5L, 1L)).thenReturn(Optional.of(alert));

    service.delete(5L);

    verify(notifications).deleteByAlertId(5L);
    verify(alerts).delete(alert);
  }

  private Alert alert(Long id) {
    var alert = new Alert();
    alert.id = id;
    alert.instrumentSymbol = "AAPL";
    alert.conditionType = "price_above";
    alert.threshold = new BigDecimal("10");
    alert.armed = true;
    alert.createdAt = LocalDateTime.parse("2026-06-26T10:15:30");
    return alert;
  }
}
