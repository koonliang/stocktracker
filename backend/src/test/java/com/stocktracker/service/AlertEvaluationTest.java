package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.Alert;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.Notification;
import com.stocktracker.persistence.AlertRepository;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class AlertEvaluationTest extends IntegrationTestSupport {
  @Inject AlertRepository alerts;
  @Inject AlertEvaluationService evaluator;
  @Inject EntityManager entityManager;

  @Test
  void firesOnceThenRearmsAfterConditionClears() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "120");

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "125", "5")));
    var fired = alerts.findById(alertId);
    assertFalse(fired.armed);
    assertEquals(1, Notification.count());
    assertEquals(
        "AAPL crossed above 120", ((Notification) Notification.findAll().firstResult()).message);

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "130", "10")));
    assertEquals(1, Notification.count());

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "110", "-5")));
    assertTrue(armed(alertId));

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "121", "1")));
    assertEquals(2, Notification.count());
  }

  @Test
  void crossingKeyIsGeneratedOnFire() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "120");
    inTransaction(() -> evaluator.evaluate(quote("AAPL", "125", "5")));

    var notification = (Notification) Notification.findAll().firstResult();
    assertFalse(notification.crossingKey.isBlank());
    assertTrue(notification.crossingKey.startsWith(alertId + "-"));
    assertEquals("AAPL", notification.instrumentSymbol);
    assertEquals("price_above", notification.conditionType);
  }

  @Test
  void multipleAlertsForSameSymbolEachFireIndependently() throws Exception {
    var alert1 = persistAlert("AAPL", "price_above", "120");
    var alert2 = persistAlert("AAPL", "price_below", "100");

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "125", "5")));
    assertEquals(1, Notification.count());

    // alert2 still armed, price above 100 so it doesn't fire
    // Clear the condition for alert1 to re-arm
    inTransaction(() -> evaluator.evaluate(quote("AAPL", "90", "-15")));
    assertEquals(2, Notification.count()); // alert1 clears, alert2 fires
  }

  @Test
  void tracksLastConditionMet() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "120");

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "125", "5")));
    entityManager.clear();
    assertTrue(alerts.findById(alertId).lastConditionMet);

    inTransaction(() -> evaluator.evaluate(quote("AAPL", "110", "-5")));
    entityManager.clear();
    assertFalse(alerts.findById(alertId).lastConditionMet);
  }

  private boolean armed(Long alertId) throws Exception {
    var holder = new boolean[1];
    inTransaction(
        () -> {
          entityManager.clear();
          holder[0] = alerts.findById(alertId).armed;
        });
    return holder[0];
  }

  private Long persistAlert(String symbol, String condition, String threshold) throws Exception {
    var holder = new Long[1];
    inTransaction(
        () -> {
          var alert = new Alert();
          alert.userId = SEED_USER_ID;
          alert.instrumentSymbol = symbol;
          alert.conditionType = condition;
          alert.threshold = new BigDecimal(threshold);
          alert.armed = true;
          alerts.persist(alert);
          holder[0] = alert.id;
        });
    return holder[0];
  }

  private InstrumentQuote quote(String symbol, String price, String changePct) {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = symbol;
    quote.price = new BigDecimal(price);
    quote.previousClose = new BigDecimal("100");
    quote.changePct = new BigDecimal(changePct);
    quote.asOf = Instant.now();
    quote.fetchedAt = Instant.now();
    quote.source = "test";
    return quote;
  }
}
