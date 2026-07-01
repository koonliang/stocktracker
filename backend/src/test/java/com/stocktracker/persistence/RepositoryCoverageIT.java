package com.stocktracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.domain.VerificationToken;
import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class RepositoryCoverageIT extends IntegrationTestSupport {
  @Inject AlertRepository alertRepository;
  @Inject AppUserRepository appUserRepository;
  @Inject FxRateRepository fxRateRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject NotificationRepository notificationRepository;
  @Inject PortfolioTransactionRepository portfolioTransactionRepository;
  @Inject QuoteRepository quoteRepository;
  @Inject SocialIdentityRepository socialIdentityRepository;
  @Inject VerificationTokenRepository verificationTokenRepository;
  @Inject WatchlistRepository watchlistRepository;

  @Test
  void appUserAndSocialIdentityRepositoriesResolveNormalizedAndDemoUsers() throws Exception {
    var ids = new Long[2];
    inTransaction(
        () -> {
          var standard = new AppUser();
          standard.email = AppUser.normalizeEmail(" DemoUser@Example.com ");
          standard.status = AppUser.Status.ACTIVE;
          standard.emailVerified = true;
          appUserRepository.persist(standard);
          ids[0] = standard.id;

          var demo = new AppUser();
          demo.email = "demo-slot@example.com";
          demo.status = AppUser.Status.ACTIVE;
          demo.emailVerified = true;
          demo.accountKind = AppUser.AccountKind.DEMO;
          demo.demoSlot = (byte) 2;
          appUserRepository.persist(demo);
          ids[1] = demo.id;

          var identity = new SocialIdentity();
          identity.userId = standard.id;
          identity.provider = SocialIdentity.Provider.GOOGLE;
          identity.providerSubject = "google-subject";
          identity.providerEmail = standard.email;
          identity.emailVerified = true;
          identity.persist();
        });

    assertTrue(appUserRepository.findByNormalizedEmail(" demouser@example.com ").isPresent());
    assertTrue(appUserRepository.existsByNormalizedEmail("DEMouser@example.com"));
    assertEquals(ids[1], appUserRepository.findDemoUserBySlot(2).orElseThrow().id);
    assertTrue(appUserRepository.listDemoUsers().stream().anyMatch(user -> ids[1].equals(user.id)));
    assertEquals(
        ids[0],
        socialIdentityRepository
            .findByProviderSubject(SocialIdentity.Provider.GOOGLE, "google-subject")
            .orElseThrow()
            .userId);
    assertTrue(
        socialIdentityRepository
            .findByProviderEmail(SocialIdentity.Provider.GOOGLE, "demouser@example.com")
            .isPresent());
  }

  @Test
  void alertAndNotificationRepositoriesFilterMutateAndDelete() throws Exception {
    var alertId = persistAlert("AAPL", "price_above", "100", true);
    var secondAlertId = persistAlert("MSFT", "price_below", "90", true);
    var unreadId = persistNotification(alertId, "AAPL", "price_above", "100", "101", false);
    var readId = persistNotification(alertId, "AAPL", "price_above", "100", "102", true);
    persistNotification(secondAlertId, "MSFT", "price_below", "90", "89", false);

    assertEquals(2, alertRepository.listForUser(SEED_USER_ID).size());
    assertEquals(1, alertRepository.listForSymbol("aapl").size());
    assertEquals(alertId, alertRepository.findByIdAndUser(alertId, SEED_USER_ID).orElseThrow().id);
    assertEquals(2, notificationRepository.listForUser(SEED_USER_ID, true).size());
    assertEquals(3, notificationRepository.listForUser(SEED_USER_ID, false).size());
    assertEquals(2, notificationRepository.listForUser(SEED_USER_ID, 2).size());
    assertEquals(
        unreadId, notificationRepository.findByIdAndUser(unreadId, SEED_USER_ID).orElseThrow().id);
    assertEquals(2L, notificationRepository.unreadCount(SEED_USER_ID));
    assertEquals(1L, notificationRepository.markRead(SEED_USER_ID, List.of(unreadId)));
    assertEquals(1L, notificationRepository.markAllRead(SEED_USER_ID));
    assertEquals(0L, notificationRepository.unreadCount(SEED_USER_ID));
    assertEquals(2L, notificationRepository.deleteByAlertId(alertId));
  }

  @Test
  void instrumentAndQuoteRepositoriesHandleLookupsAndEmptyCollections() throws Exception {
    persistInstrument("ZZZT", "Zeta Test", "NASDAQ", "USD");
    inTransaction(
        () -> {
          var firstBar = new InstrumentPriceBar();
          firstBar.instrumentSymbol = "ZZZT";
          firstBar.tradeDate = LocalDate.parse("2026-06-20");
          firstBar.openPrice = new BigDecimal("10");
          firstBar.highPrice = new BigDecimal("11");
          firstBar.lowPrice = new BigDecimal("9");
          firstBar.closePrice = new BigDecimal("10.5");
          firstBar.volume = 100L;
          firstBar.persist();

          var secondBar = new InstrumentPriceBar();
          secondBar.instrumentSymbol = "ZZZT";
          secondBar.tradeDate = LocalDate.parse("2026-06-21");
          secondBar.openPrice = new BigDecimal("11");
          secondBar.highPrice = new BigDecimal("12");
          secondBar.lowPrice = new BigDecimal("10");
          secondBar.closePrice = new BigDecimal("11.5");
          secondBar.volume = 120L;
          secondBar.persist();

          var stat = new InstrumentStat();
          stat.instrumentSymbol = "ZZZT";
          stat.openPrice = new BigDecimal("10");
          stat.highPrice = new BigDecimal("12");
          stat.lowPrice = new BigDecimal("9");
          stat.previousClose = new BigDecimal("10.5");
          stat.volume = 120L;
          stat.week52High = new BigDecimal("20");
          stat.week52Low = new BigDecimal("5");
          stat.marketCap = 1_000_000L;
          stat.peRatio = new BigDecimal("15");
          stat.asOfDate = LocalDate.parse("2026-06-21");
          stat.persist();

          var quote = new InstrumentQuote();
          quote.instrumentSymbol = "ZZZT";
          quote.price = new BigDecimal("11.5");
          quote.previousClose = new BigDecimal("10.5");
          quote.changeAmount = new BigDecimal("1");
          quote.changePct = new BigDecimal("9.5238");
          quote.asOf = Instant.parse("2026-06-21T00:00:00Z");
          quote.fetchedAt = Instant.parse("2026-06-21T01:00:00Z");
          quote.source = "test";
          quote.stale = false;
          quote.persist();
        });

    assertTrue(instrumentRepository.findBySymbol("zzzt").isPresent());
    assertTrue(instrumentRepository.existsSymbol("zzzt"));
    assertFalse(instrumentRepository.existsSymbol("missing"));
    assertFalse(instrumentRepository.search("zeta", 5).isEmpty());
    assertEquals(Set.of("ZZZT"), instrumentRepository.findBySymbols(List.of("ZZZT")).keySet());
    assertTrue(instrumentRepository.findBySymbols(List.of()).isEmpty());
    assertEquals(2, instrumentRepository.listPriceBars("zzzt").size());
    assertEquals(2, instrumentRepository.listPriceBars(List.of("ZZZT")).size());
    assertTrue(instrumentRepository.listPriceBars(List.of()).isEmpty());
    assertTrue(
        instrumentRepository.findPriceBar("zzzt", LocalDate.parse("2026-06-21")).isPresent());
    assertTrue(instrumentRepository.findStat("zzzt").isPresent());

    assertTrue(quoteRepository.findBySymbol("zzzt").isPresent());
    assertEquals(1, quoteRepository.findBySymbols(List.of("ZZZT")).size());
    assertTrue(quoteRepository.findBySymbols(List.of()).isEmpty());
    assertEquals("ZZZT", quoteRepository.findOrNew("zzzt").instrumentSymbol);
    assertEquals("MISS", quoteRepository.findOrNew("miss").instrumentSymbol);
  }

  @Test
  void fxVerificationWatchlistAndTransactionRepositoriesCoverRemainingBranches() throws Exception {
    var transactionId =
        persistTransaction("2026-06-20", "AAPL", "buy", "1", "100", "0", null, null);
    persistTransaction("2026-06-21", "AAPL", "buy", "1", "101", "0", "101", "USD");
    persistFxRate("USD", "SGD", "2026-06-21", "1.35");

    var watchlistId = new Long[1];
    inTransaction(
        () -> {
          var watchlist = new Watchlist();
          watchlist.userId = SEED_USER_ID;
          watchlist.name = "Tech Picks";
          watchlist.persist();
          watchlistId[0] = watchlist.id;

          var item = new WatchlistItem();
          item.watchlistId = watchlist.id;
          item.instrumentSymbol = "AAPL";
          item.displayOrder = 1;
          item.persist();

          var current = new VerificationToken();
          current.userId = SEED_USER_ID;
          current.purpose = VerificationToken.Purpose.EMAIL_VERIFICATION;
          current.tokenHash = "usable-hash";
          current.expiresAt = LocalDateTime.now().plusDays(1);
          current.persist();

          var expired = new VerificationToken();
          expired.userId = SEED_USER_ID;
          expired.purpose = VerificationToken.Purpose.EMAIL_VERIFICATION;
          expired.tokenHash = "expired-hash";
          expired.expiresAt = LocalDateTime.now().minusDays(1);
          expired.persist();
        });

    assertTrue(fxRateRepository.find("usd", "sgd", LocalDate.parse("2026-06-21")).isPresent());
    assertTrue(
        fxRateRepository
            .findLatestOnOrBefore("usd", "sgd", LocalDate.parse("2026-06-22"))
            .isPresent());
    assertEquals(
        "USD",
        fxRateRepository.findOrNew("usd", "sgd", LocalDate.parse("2026-06-21")).baseCurrency);
    assertEquals(
        "EUR",
        fxRateRepository.findOrNew("eur", "jpy", LocalDate.parse("2026-06-22")).baseCurrency);
    var inserted = new int[1];
    inTransaction(
        () ->
            inserted[0] =
                fxRateRepository.insertIgnore(
                    "eur",
                    "jpy",
                    LocalDate.parse("2026-06-22"),
                    new BigDecimal("170"),
                    "test",
                    false));
    assertEquals(1, inserted[0]);

    assertTrue(verificationTokenRepository.findByHash("usable-hash").isPresent());
    assertTrue(
        verificationTokenRepository
            .findLatestUsable(
                SEED_USER_ID, VerificationToken.Purpose.EMAIL_VERIFICATION, LocalDateTime.now())
            .isPresent());
    var deletedTokens = new long[1];
    inTransaction(
        () -> {
          verificationTokenRepository.supersedePrior(
              SEED_USER_ID, VerificationToken.Purpose.EMAIL_VERIFICATION, LocalDateTime.now());
          deletedTokens[0] =
              verificationTokenRepository.deleteExpiredOrConsumed(LocalDateTime.now().plusDays(2));
        });
    assertEquals(2, deletedTokens[0]);

    assertTrue(
        watchlistRepository.findByUserAndNameIgnoreCase(SEED_USER_ID, "tech picks").isPresent());
    assertTrue(watchlistRepository.findByIdAndUser(watchlistId[0], SEED_USER_ID).isPresent());
    assertEquals(1, watchlistRepository.listByUserUpdatedAt(SEED_USER_ID).size());
    assertEquals(1, watchlistRepository.listItems(watchlistId[0]).size());

    assertEquals(2, portfolioTransactionRepository.listAscending(SEED_USER_ID).size());
    assertEquals(2, portfolioTransactionRepository.listDescending(SEED_USER_ID).size());
    assertTrue(
        portfolioTransactionRepository.findByIdAndUser(transactionId, SEED_USER_ID).isPresent());
    assertEquals(1, portfolioTransactionRepository.findMissingCurrency(SEED_USER_ID).size());
    assertEquals(1, portfolioTransactionRepository.countMissingCurrency(SEED_USER_ID));
    assertNotNull(portfolioTransactionRepository.listAscending().getFirst().id);
  }
}
