package com.stocktracker.service;

import com.stocktracker.domain.Alert;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.Notification;
import com.stocktracker.persistence.AlertRepository;
import com.stocktracker.persistence.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@ApplicationScoped
public class AlertEvaluationService {
  @Inject AlertRepository alerts;
  @Inject NotificationRepository notifications;
  @Inject Clock clock;

  @Transactional
  public void evaluate(InstrumentQuote quote) {
    if (quote == null || quote.instrumentSymbol == null) {
      return;
    }
    for (var alert : alerts.listForSymbol(quote.instrumentSymbol)) {
      var matches = matches(alert, quote);
      var wasMet = Boolean.TRUE.equals(alert.lastConditionMet);
      var isNewCrossing = !wasMet && matches && alert.armed;
      var didClear = wasMet && !matches;

      if (isNewCrossing) {
        fire(alert, quote);
      }

      alert.lastConditionMet = matches;
      if (didClear && !alert.armed) {
        alert.armed = true;
        alert.lastClearedAt = clock.instant();
      }
      if (matches) {
        alert.lastTriggeredAt = clock.instant();
      }
      alerts.persist(alert);
    }
  }

  private boolean matches(Alert alert, InstrumentQuote quote) {
    return switch (alert.conditionType) {
      case "price_above" -> compare(quote.price, alert.threshold) > 0;
      case "price_below" -> compare(quote.price, alert.threshold) < 0;
      case "pct_change" -> compare(quote.changePct, alert.threshold) >= 0;
      default -> false;
    };
  }

  private void fire(Alert alert, InstrumentQuote quote) {
    alert.armed = false;
    alert.lastTriggeredAt = clock.instant();
    alerts.persist(alert);

    var crossingKey = alert.id + "-" + clock.instant().toEpochMilli();

    var notification = new Notification();
    notification.userId = alert.userId;
    notification.alertId = alert.id;
    notification.instrumentSymbol = alert.instrumentSymbol;
    notification.conditionType = alert.conditionType;
    notification.threshold = alert.threshold;
    notification.observedValue = quote.price != null ? quote.price : quote.changePct;
    notification.observedCurrency = null;
    notification.triggeredAt = LocalDateTime.now(clock);
    notification.crossingKey = crossingKey;
    notification.message =
        "%s %s %s"
            .formatted(
                alert.instrumentSymbol,
                label(alert.conditionType),
                alert.threshold.stripTrailingZeros().toPlainString());
    notifications.persist(notification);
  }

  private String label(String conditionType) {
    return switch (conditionType) {
      case "price_above" -> "crossed above";
      case "price_below" -> "crossed below";
      case "pct_change" -> "moved by";
      default -> "matched";
    };
  }

  private int compare(BigDecimal left, BigDecimal right) {
    if (left == null || right == null) {
      return 0;
    }
    return left.compareTo(right);
  }
}
