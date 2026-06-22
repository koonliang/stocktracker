package com.stocktracker.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.AppUser;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.service.provider.MarketDataProvider;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class DemoUserAuthResourceTest extends IntegrationTestSupport {
  @BeforeEach
  void resetDemoUsers() throws Exception {
    inTransaction(() -> AppUser.delete("accountKind", AppUser.AccountKind.DEMO));
  }

  @Test
  void createsAndListsDemoUsers() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("label", "Demo User 1"))
        .when()
        .post("/api/auth/demo-users")
        .then()
        .statusCode(201)
        .body("token", notNullValue())
        .body("demoUser.slot", equalTo(1));

    given()
        .when()
        .get("/api/auth/demo-users")
        .then()
        .statusCode(200)
        .body("users", hasSize(1))
        .body("canCreate", equalTo(true));
  }

  @Test
  void logsIntoExistingDemoUserBySlot() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of())
        .when()
        .post("/api/auth/demo-users")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/auth/demo-users/1/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("demoUser.label", equalTo("Demo User 1"));
  }
}

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestProfile(DemoUserAuthResourceLiveProviderTest.LiveProviderProfile.class)
class DemoUserAuthResourceLiveProviderTest extends IntegrationTestSupport {
  public static class LiveProviderProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("stocktracker.marketdata.provider", "yahoo");
    }
  }

  @Inject QuoteRepository quoteRepository;

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
            return List.of();
          }

          @Override
          public List<ProviderDailyBar> dailyHistoryMax(String symbol) {
            return List.of();
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
          com.stocktracker.domain.InstrumentPriceBar.deleteAll();
          com.stocktracker.domain.InstrumentStat.deleteAll();
        });
  }

  @Test
  void createsDemoUserWithoutRehydratingSharedMarketData() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("label", "Live Demo"))
        .when()
        .post("/api/auth/demo-users")
        .then()
        .statusCode(201)
        .body("demoUser.slot", equalTo(1));

    assertFalse(
        given()
            .when()
            .get("/api/auth/demo-users")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("users")
            .isEmpty());
    assertTrue(quoteRepository.listAll().isEmpty());
  }
}
