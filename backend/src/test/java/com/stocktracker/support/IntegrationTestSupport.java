package com.stocktracker.support;

import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;

public abstract class IntegrationTestSupport {
  /** Deterministic seed account created by migration V2; owns test-persisted data. */
  protected static final String SEED_USER_EMAIL = "seed@stocktracker.local";

  protected static final long SEED_USER_ID = 1L;

  @Inject UserTransaction userTransaction;
  @Inject PortfolioTransactionRepository transactionRepository;

  @BeforeEach
  void resetMutableData() throws Exception {
    inTransaction(
        () -> {
          WatchlistItem.deleteAll();
          Watchlist.deleteAll();
          PortfolioTransaction.deleteAll();
        });
  }

  protected Long persistTransaction(
      String date, String ticker, String type, String quantity, String price, String fees)
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
          transaction.source = "MANUAL";
          transactionRepository.persist(transaction);
          holder[0] = transaction.id;
        });
    return holder[0];
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
