package com.stocktracker.support;

import com.stocktracker.domain.Alert;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.FxRate;
import com.stocktracker.domain.Notification;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import com.stocktracker.persistence.AlertRepository;
import com.stocktracker.persistence.FxRateRepository;
import com.stocktracker.persistence.NotificationRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;

public abstract class IntegrationTestSupport {
  /** Deterministic seed account created by migration V2; owns test-persisted data. */
  protected static final String SEED_USER_EMAIL = "seed@stocktracker.local";

  protected static final long SEED_USER_ID = 1L;

  @Inject UserTransaction userTransaction;
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject AlertRepository alertRepository;
  @Inject NotificationRepository notificationRepository;
  @Inject FxRateRepository fxRateRepository;

  private long alertIdCounter = 1000;

  @BeforeEach
  void resetMutableData() throws Exception {
    inTransaction(
        () -> {
          WatchlistItem.deleteAll();
          Watchlist.deleteAll();
          Notification.deleteAll();
          Alert.deleteAll();
          PortfolioTransaction.deleteAll();
          FxRate.deleteAll();
          var seedUser = AppUser.<AppUser>findById(SEED_USER_ID);
          if (seedUser != null) {
            seedUser.baseCurrency = "USD";
          }
        });
  }

  protected Long persistTransaction(
      String date, String ticker, String type, String quantity, String price, String fees)
      throws Exception {
    return persistTransaction(date, ticker, type, quantity, price, fees, null, null);
  }

  protected Long persistTransaction(
      String date,
      String ticker,
      String type,
      String quantity,
      String price,
      String fees,
      String amount,
      String currency)
      throws Exception {
    var holder = new Long[1];
    inTransaction(
        () -> {
          var transaction = new PortfolioTransaction();
          transaction.userId = SEED_USER_ID;
          transaction.tradeDate = LocalDate.parse(date);
          transaction.instrumentSymbol = ticker;
          transaction.transactionType = type;
          transaction.quantity = new BigDecimal(quantity);
          transaction.price = new BigDecimal(price);
          transaction.fees = new BigDecimal(fees);
          transaction.amount = amount == null ? null : new BigDecimal(amount);
          transaction.currency = currency;
          transaction.source = "MANUAL";
          transactionRepository.persist(transaction);
          holder[0] = transaction.id;
        });
    return holder[0];
  }

  protected Long persistLegacyTransaction(
      String date, String ticker, String type, String quantity, String price, String fees)
      throws Exception {
    return persistTransaction(date, ticker, type, quantity, price, fees, null, null);
  }

  protected Long persistLegacyTransaction(
      String date,
      String ticker,
      String type,
      String quantity,
      String price,
      String fees,
      String amount)
      throws Exception {
    return persistTransaction(date, ticker, type, quantity, price, fees, amount, null);
  }

  protected Long persistAlert(String symbol, String conditionType, String threshold, boolean armed)
      throws Exception {
    var holder = new Long[1];
    inTransaction(
        () -> {
          var alert = new Alert();
          alert.userId = SEED_USER_ID;
          alert.instrumentSymbol = symbol;
          alert.conditionType = conditionType;
          alert.threshold = new BigDecimal(threshold);
          alert.armed = armed;
          alertRepository.persist(alert);
          holder[0] = alert.id;
        });
    return holder[0];
  }

  protected Long persistNotification(
      Long alertId,
      String symbol,
      String conditionType,
      String threshold,
      String observedValue,
      boolean read)
      throws Exception {
    var holder = new Long[1];
    inTransaction(
        () -> {
          var notification = new Notification();
          notification.userId = SEED_USER_ID;
          notification.alertId = alertId;
          notification.instrumentSymbol = symbol;
          notification.conditionType = conditionType;
          notification.threshold = new BigDecimal(threshold);
          notification.observedValue = new BigDecimal(observedValue);
          notification.observedCurrency = "USD";
          notification.triggeredAt = LocalDateTime.now();
          notification.read = read;
          notification.message =
              String.format(
                  "%s %s %s",
                  symbol,
                  conditionType.equals("price_above") ? "crossed above" : "crossed below",
                  threshold);
          notification.crossingKey =
              String.format("%d-%s-%s", alertId, symbol, Instant.now().toEpochMilli());
          notificationRepository.persist(notification);
          holder[0] = notification.id;
        });
    return holder[0];
  }

  protected void persistFxRate(
      String baseCurrency, String quoteCurrency, String rateDate, String rate) throws Exception {
    inTransaction(
        () -> {
          var fxRate = new FxRate();
          fxRate.baseCurrency = baseCurrency;
          fxRate.quoteCurrency = quoteCurrency;
          fxRate.rateDate = LocalDate.parse(rateDate);
          fxRate.rate = new BigDecimal(rate);
          fxRate.source = "stub";
          fxRateRepository.persist(fxRate);
        });
  }

  protected void inTransaction(CheckedAction action) throws Exception {
    userTransaction.begin();
    try {
      action.run();
      userTransaction.commit();
    } catch (Exception exception) {
      userTransaction.rollback();
      throw exception;
    }
  }

  @FunctionalInterface
  protected interface CheckedAction {
    void run() throws Exception;
  }
}
