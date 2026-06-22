package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.service.provider.MarketDataProvider;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCreateRequest;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class DemoUserServiceTest extends IntegrationTestSupport {
  @Inject DemoUserService service;

  @BeforeEach
  void resetDemoUsers() throws Exception {
    inTransaction(() -> AppUser.delete("accountKind", AppUser.AccountKind.DEMO));
  }

  @Test
  void createUsesLowestAvailableSlotAndNormalizesBlankLabels() throws Exception {
    var first = service.create(new DemoUserCreateRequest("   "));
    var second = service.create(new DemoUserCreateRequest("Growth Demo"));

    inTransaction(
        () -> {
          PortfolioTransaction.delete("userId", first.id);
          AppUser.deleteById(first.id);
        });

    var replacement = service.create(new DemoUserCreateRequest(null));

    Assertions.assertEquals(1, replacement.demoSlot.intValue());
    Assertions.assertEquals("Demo User 1", service.labelFor(replacement));
    Assertions.assertEquals("Growth Demo", service.labelFor(second));
    Assertions.assertEquals(AppUser.AccountKind.DEMO, replacement.accountKind);
    Assertions.assertTrue(replacement.emailVerified);
  }

  @Test
  void catalogIsSortedAndReportsWhenMoreDemoUsersCanBeCreated() {
    service.create(new DemoUserCreateRequest("Demo User 1"));
    service.create(new DemoUserCreateRequest("Demo User 2"));

    var catalog = service.catalog();

    Assertions.assertEquals(2, catalog.users().size());
    Assertions.assertEquals(1, catalog.users().get(0).slot());
    Assertions.assertEquals(2, catalog.users().get(1).slot());
    Assertions.assertEquals(3, catalog.maxUsers());
    Assertions.assertTrue(catalog.canCreate());
  }

  @Test
  void createRejectsFourthDemoUser() {
    service.create(new DemoUserCreateRequest("Demo User 1"));
    service.create(new DemoUserCreateRequest("Demo User 2"));
    service.create(new DemoUserCreateRequest("Demo User 3"));

    var error =
        Assertions.assertThrows(
            ApiException.class, () -> service.create(new DemoUserCreateRequest("Demo User 4")));

    Assertions.assertEquals(409, error.status().getStatusCode());
    Assertions.assertEquals("DEMO_USER_LIMIT_REACHED", error.code());
  }

  @Test
  void loginUpdatesDemoActivationAndLastLoginTimestamps() throws Exception {
    var created = service.create(new DemoUserCreateRequest("Demo User 1"));

    Thread.sleep(5L);
    var loggedIn = service.login(created.demoSlot.intValue());

    Assertions.assertNotNull(loggedIn.lastLoginAt);
    Assertions.assertNotNull(loggedIn.demoLastActivatedAt);

    var reloaded = new AppUser[1];
    inTransaction(() -> reloaded[0] = AppUser.findById(loggedIn.id));
    Assertions.assertNotNull(reloaded[0]);
    Assertions.assertNotNull(reloaded[0].lastLoginAt);
    Assertions.assertNotNull(reloaded[0].demoLastActivatedAt);
  }
}

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestProfile(DemoUserServiceLiveProviderTest.LiveProviderProfile.class)
class DemoUserServiceLiveProviderTest extends IntegrationTestSupport {
  public static class LiveProviderProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("stocktracker.marketdata.provider", "yahoo");
    }
  }

  @Inject DemoUserService service;
  @Inject QuoteRepository quoteRepository;
  @Inject InstrumentRepository instrumentRepository;

  @BeforeEach
  void resetDemoUsersAndProvider() throws Exception {
    QuarkusMock.installMockForType(
        new MarketDataProvider() {
          @Override
          public List<ProviderQuote> latestQuotes(Collection<String> symbols) {
            return symbols.stream()
                .map(
                    symbol ->
                        new ProviderQuote(
                            symbol,
                            BigDecimal.valueOf(100),
                            BigDecimal.valueOf(95),
                            Instant.parse("2026-06-22T00:00:00Z")))
                .toList();
          }

          @Override
          public List<ProviderDailyBar> dailyHistory(String symbol, java.time.LocalDate from) {
            return List.of(
                new ProviderDailyBar(symbol, java.time.LocalDate.parse("2026-06-20"), BigDecimal.valueOf(91)),
                new ProviderDailyBar(symbol, java.time.LocalDate.parse("2026-06-21"), BigDecimal.valueOf(94)));
          }

          @Override
          public List<ProviderDailyBar> dailyHistoryMax(String symbol) {
            return List.of(
                new ProviderDailyBar(
                    symbol, java.time.LocalDate.parse("2026-06-20"), BigDecimal.valueOf(91)),
                new ProviderDailyBar(
                    symbol, java.time.LocalDate.parse("2026-06-21"), BigDecimal.valueOf(94)));
          }

          @Override
          public List<ProviderSymbol> searchSymbols(String query) {
            return List.of();
          }

          @Override
          public ProviderSnapshot latestSnapshot(String symbol) {
            return new ProviderSnapshot(
                symbol,
                BigDecimal.valueOf(96),
                BigDecimal.valueOf(101),
                BigDecimal.valueOf(95),
                BigDecimal.valueOf(95),
                123456L,
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(70),
                999999L,
                BigDecimal.valueOf(25),
                java.time.LocalDate.parse("2026-06-22"));
          }
        },
        MarketDataProvider.class);

    inTransaction(
        () -> {
          AppUser.delete("accountKind", AppUser.AccountKind.DEMO);
          com.stocktracker.domain.InstrumentQuote.deleteAll();
          InstrumentPriceBar.deleteAll();
          InstrumentStat.deleteAll();
        });
  }

  @Test
  void createDoesNotRepopulateSharedMarketDataWhenLiveProviderModeIsEnabled() {
    var created = service.create(new DemoUserCreateRequest("Live Demo"));

    Assertions.assertNotNull(created.id);
    Assertions.assertTrue(quoteRepository.listAll().isEmpty());
    Assertions.assertTrue(instrumentRepository.listPriceBars("NVDA").isEmpty());
    Assertions.assertTrue(instrumentRepository.findStat("NVDA").isEmpty());
  }
}
