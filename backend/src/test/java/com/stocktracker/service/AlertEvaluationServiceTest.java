package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.Alert;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.Notification;
import com.stocktracker.persistence.AlertRepository;
import com.stocktracker.persistence.NotificationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AlertEvaluationServiceTest {
  private final AlertRepository alerts = Mockito.mock(AlertRepository.class);
  private final NotificationRepository notifications = Mockito.mock(NotificationRepository.class);
  private AlertEvaluationService service;

  @BeforeEach
  void setUp() {
    service = new AlertEvaluationService();
    service.alerts = alerts;
    service.notifications = notifications;
    service.clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
  }

  @Test
  void ignoresNullQuote() {
    service.evaluate(null);

    verify(alerts, never()).listForSymbol(any());
  }

  @Test
  void firesNotificationOnNewCrossing() {
    var alert = alert("price_above", "100", true, false);
    alert.id = 7L;
    when(alerts.listForSymbol("AAPL")).thenReturn(List.of(alert));

    service.evaluate(quote("AAPL", "101", null));

    assertFalse(alert.armed);
    assertTrue(alert.lastConditionMet);
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).persist(captor.capture());
    assertEquals("AAPL", captor.getValue().instrumentSymbol);
    assertEquals("AAPL crossed above 100", captor.getValue().message);
    assertNotNull(captor.getValue().crossingKey);
  }

  @Test
  void rearmsAlertWhenConditionClears() {
    var alert = alert("price_below", "90", false, true);
    when(alerts.listForSymbol("AAPL")).thenReturn(List.of(alert));

    service.evaluate(quote("AAPL", "95", null));

    assertTrue(alert.armed);
    assertFalse(alert.lastConditionMet);
    assertNotNull(alert.lastClearedAt);
  }

  @Test
  void supportsPctChangeAlerts() {
    var alert = alert("pct_change", "5", true, false);
    alert.id = 9L;
    when(alerts.listForSymbol("AAPL")).thenReturn(List.of(alert));

    service.evaluate(quote("AAPL", null, "5.1"));

    verify(notifications).persist(any(Notification.class));
  }

  private Alert alert(String type, String threshold, boolean armed, boolean lastMet) {
    var alert = new Alert();
    alert.userId = 1L;
    alert.instrumentSymbol = "AAPL";
    alert.conditionType = type;
    alert.threshold = new BigDecimal(threshold);
    alert.armed = armed;
    alert.lastConditionMet = lastMet;
    return alert;
  }

  private InstrumentQuote quote(String symbol, String price, String changePct) {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = symbol;
    quote.price = price == null ? null : new BigDecimal(price);
    quote.changePct = changePct == null ? null : new BigDecimal(changePct);
    return quote;
  }
}
