package com.stocktracker.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.config.NonProdAuthConfig;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.service.MarketDataService;
import com.stocktracker.service.provider.ProviderConfig;
import jakarta.enterprise.inject.Vetoed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DevDataBootstrapTest {
  private final InstrumentRepository instrumentRepository =
      Mockito.mock(InstrumentRepository.class);
  private final PortfolioTransactionRepository transactionRepository =
      Mockito.mock(PortfolioTransactionRepository.class);
  private final AppUserRepository appUserRepository = Mockito.mock(AppUserRepository.class);
  private final NonProdAuthConfig nonProdAuthConfig = Mockito.mock(NonProdAuthConfig.class);
  private final MarketDataService marketDataService = Mockito.mock(MarketDataService.class);
  private final ProviderConfig providerConfig = Mockito.mock(ProviderConfig.class);

  private TestDevDataBootstrap bootstrap;

  @BeforeEach
  void setUp() {
    bootstrap = new TestDevDataBootstrap();
    bootstrap.instrumentRepository = instrumentRepository;
    bootstrap.transactionRepository = transactionRepository;
    bootstrap.appUserRepository = appUserRepository;
    bootstrap.nonProdAuthConfig = nonProdAuthConfig;
    bootstrap.marketDataService = marketDataService;
    bootstrap.providerConfig = providerConfig;
    bootstrap.enabled = true;
    when(instrumentRepository.existsSymbol(Mockito.anyString())).thenReturn(true);
    Mockito.doAnswer(
            invocation -> {
              bootstrap.persistedTransactions.add(
                  invocation.getArgument(0, com.stocktracker.domain.PortfolioTransaction.class));
              return null;
            })
        .when(transactionRepository)
        .persist(Mockito.any(com.stocktracker.domain.PortfolioTransaction.class));
  }

  @Test
  void onStartSkipsWhenDisabled() throws Exception {
    bootstrap.enabled = false;

    bootstrap.onStart(null);

    assertFalse(bootstrap.isBootstrappingMarketData());
    assertEquals(0, bootstrap.bootstrappedSymbols.size());
  }

  @Test
  void refreshDemoUserPortfolioIgnoresNonDemoUsers() throws Exception {
    bootstrap.refreshDemoUserPortfolio(null);

    var standard = new AppUser();
    standard.id = 4L;
    standard.accountKind = AppUser.AccountKind.STANDARD;
    bootstrap.refreshDemoUserPortfolio(standard);

    assertEquals(List.of(), bootstrap.deletedUserIds);
    assertEquals(List.of(), bootstrap.insertCalls);
  }

  @Test
  void refreshDemoUserPortfolioReplacesTransactionsForDemoUser() throws Exception {
    var demo = new AppUser();
    demo.id = 5L;
    demo.accountKind = AppUser.AccountKind.DEMO;
    demo.demoSeedProfile = "growth";
    bootstrap.seedProfiles.put("growth", List.of(seedRow("msft")));

    bootstrap.refreshDemoUserPortfolio(demo);

    assertEquals(List.of(5L), bootstrap.deletedUserIds);
    assertEquals(List.of("5:growth"), bootstrap.insertCalls);
  }

  @Test
  void onStartBootstrapsDefaultSymbolsWhenDemoUsersDisabled() throws Exception {
    when(nonProdAuthConfig.demoUsersEnabled()).thenReturn(false);
    bootstrap.seedUser = user(1L, AppUser.AccountKind.STANDARD, "seed");
    bootstrap.seedProfiles.put("seed", List.of(seedRow("aapl"), seedRow("msft")));

    bootstrap.onStart(null);

    assertEquals(1, bootstrap.seedPortfolioCalls);
    assertEquals(1, bootstrap.verifiedUserCalls);
    assertEquals(Set.of("AAPL", "MSFT"), bootstrap.bootstrappedSymbols);
    assertFalse(bootstrap.isBootstrappingMarketData());
  }

  @Test
  void onStartRefreshesDemoUsersAndBootstrapsUnionOfSymbols() throws Exception {
    when(nonProdAuthConfig.demoUsersEnabled()).thenReturn(true);
    var firstDemo = user(7L, AppUser.AccountKind.DEMO, "growth");
    var secondDemo = user(8L, AppUser.AccountKind.DEMO, "income");
    when(appUserRepository.listDemoUsers()).thenReturn(List.of(firstDemo, secondDemo));
    bootstrap.seedUser = user(1L, AppUser.AccountKind.STANDARD, "seed");
    bootstrap.seedProfiles.put("seed", List.of(seedRow("aapl")));
    bootstrap.seedProfiles.put("growth", List.of(seedRow("msft")));
    bootstrap.seedProfiles.put("income", List.of(seedRow("goog"), seedRow("aapl")));

    bootstrap.onStart(null);

    assertEquals(List.of(firstDemo, secondDemo), bootstrap.refreshedUsers);
    assertEquals(Set.of("AAPL", "MSFT", "GOOG"), bootstrap.bootstrappedSymbols);
    assertFalse(bootstrap.isBootstrappingMarketData());
  }

  @Test
  void ensureVerifiedUserCreatesCredentialOnlyWhenMissing() {
    when(appUserRepository.findByNormalizedEmail("empty@stocktracker.local"))
        .thenReturn(Optional.empty());
    Mockito.doAnswer(
            invocation -> {
              var user = invocation.<AppUser>getArgument(0);
              user.id = 22L;
              return null;
            })
        .when(appUserRepository)
        .persist(Mockito.any(AppUser.class));
    bootstrap.credentialCounts.put(22L, 0L);

    bootstrap.ensureVerifiedUser("empty@stocktracker.local", "pw");

    assertEquals(1, bootstrap.persistedCredentials.size());
    assertEquals("hash:pw", bootstrap.persistedCredentials.getFirst().passwordHash);
  }

  @Test
  void ensureLegacySeedUserReturnsExistingCredentialWithoutPersisting() {
    var seedUser = user(11L, AppUser.AccountKind.STANDARD, "seed");
    when(appUserRepository.findByNormalizedEmail("seed@stocktracker.local"))
        .thenReturn(Optional.of(seedUser));
    bootstrap.credentialCounts.put(11L, 1L);

    var resolved = bootstrap.ensureLegacySeedUser();

    assertEquals(seedUser, resolved);
    assertTrue(bootstrap.persistedCredentials.isEmpty());
  }

  @Test
  void ensureLegacySeedUserCreatesCredentialWhenMissing() {
    var seedUser = user(12L, AppUser.AccountKind.STANDARD, "seed");
    when(appUserRepository.findByNormalizedEmail("seed@stocktracker.local"))
        .thenReturn(Optional.of(seedUser));
    bootstrap.credentialCounts.put(12L, 0L);

    var resolved = bootstrap.ensureLegacySeedUser();

    assertEquals(seedUser, resolved);
    assertEquals(1, bootstrap.persistedCredentials.size());
    assertEquals(12L, bootstrap.persistedCredentials.getFirst().userId);
    assertEquals("hash:DevPass123!", bootstrap.persistedCredentials.getFirst().passwordHash);
  }

  @Test
  void ensureSeedPortfolioSkipsNullExistingAndSeedsMissingTransactions() throws Exception {
    bootstrap.ensureSeedPortfolio(null);

    var unsaved = new AppUser();
    bootstrap.ensureSeedPortfolio(unsaved);

    var seeded = user(21L, AppUser.AccountKind.STANDARD, "seed");
    bootstrap.transactionCounts.put(21L, 1L);
    bootstrap.ensureSeedPortfolio(seeded);

    bootstrap.transactionCounts.put(21L, 0L);
    bootstrap.seedProfiles.put("seed", List.of(seedRow("aapl")));
    bootstrap.ensureSeedPortfolio(seeded);

    assertEquals(List.of("21:seed"), bootstrap.insertCalls);
  }

  @Test
  void ensureVerifiedUserSkipsCredentialCreationWhenAlreadyPresent() {
    var existing = user(41L, AppUser.AccountKind.STANDARD, "seed");
    when(appUserRepository.findByNormalizedEmail("existing@stocktracker.local"))
        .thenReturn(Optional.of(existing));
    bootstrap.credentialCounts.put(41L, 1L);

    bootstrap.ensureVerifiedUser("existing@stocktracker.local", "pw");

    assertTrue(bootstrap.persistedCredentials.isEmpty());
  }

  @Test
  void loadDemoTransactionsFallsBackToDefaultProfile() throws Exception {
    bootstrap.seedProfiles.put("seed", List.of(seedRow("aapl")));

    assertEquals(bootstrap.seedProfiles.get("seed"), bootstrap.loadDemoTransactions(" "));
    assertEquals(bootstrap.seedProfiles.get("seed"), bootstrap.loadDemoTransactions("unknown"));
  }

  @Test
  void loadSeedSymbolsNormalizesCaseAndLoadAllSeedSymbolsMergesProfiles() throws Exception {
    bootstrap.seedProfiles.put("seed", List.of(seedRow("aapl"), seedRow("msft")));
    bootstrap.seedProfiles.put("growth", List.of(seedRow("goog")));
    when(appUserRepository.listDemoUsers())
        .thenReturn(List.of(user(5L, AppUser.AccountKind.DEMO, "growth")));

    assertEquals(Set.of("AAPL", "MSFT"), bootstrap.loadSeedSymbols("seed"));
    assertEquals(Set.of("AAPL", "MSFT", "GOOG"), bootstrap.loadAllSeedSymbols());
  }

  @Test
  void insertTransactionsRejectsUnknownSymbolsAndPersistsMappedRows() throws Exception {
    bootstrap.seedProfiles.put(
        "seed",
        List.of(
            Map.of(
                "ticker", "aapl",
                "date", "2026-01-02",
                "type", "BUY",
                "quantity", "3",
                "price", "100.50",
                "fees", "1.25")));

    when(instrumentRepository.existsSymbol("AAPL")).thenReturn(true);

    var symbols = bootstrap.insertTransactions(77L, "seed");

    assertEquals(Set.of("AAPL"), symbols);
    assertEquals(1, bootstrap.persistedTransactions.size());
    var transaction = bootstrap.persistedTransactions.getFirst();
    assertEquals(77L, transaction.userId);
    assertEquals("AAPL", transaction.instrumentSymbol);
    assertEquals(new BigDecimal("3"), transaction.quantity);
    assertEquals(new BigDecimal("100.50"), transaction.price);
    assertEquals(new BigDecimal("1.25"), transaction.fees);

    bootstrap.seedProfiles.put(
        "seed",
        List.of(
            Map.of(
                "ticker", "missing",
                "date", "2026-01-02",
                "type", "BUY",
                "quantity", "1",
                "price", "1",
                "fees", "0")));
    when(instrumentRepository.existsSymbol("MISSING")).thenReturn(false);

    assertThrows(IllegalStateException.class, () -> bootstrap.insertTransactions(77L, "seed"));
  }

  @Test
  void bootstrapSeededMarketDataRequiresLiveProviderAndSymbols() {
    bootstrap.bootstrapSeededMarketData(Set.of());
    verify(marketDataService, never()).bootstrapTrackedSymbolsAndAnalysis(Mockito.anySet());

    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    bootstrap.bootstrapSeededMarketData(Set.of("AAPL"));
    verify(marketDataService, never()).bootstrapTrackedSymbolsAndAnalysis(Mockito.anySet());

    when(providerConfig.isLiveMarketDataProvider()).thenReturn(true);
    bootstrap.bootstrapSeededMarketData(Set.of("AAPL", "MSFT"));
    verify(marketDataService).bootstrapTrackedSymbolsAndAnalysis(Set.of("AAPL", "MSFT"));
  }

  private AppUser user(Long id, AppUser.AccountKind kind, String profile) {
    var user = new AppUser();
    user.id = id;
    user.accountKind = kind;
    user.demoSeedProfile = profile;
    return user;
  }

  private Map<String, Object> seedRow(String ticker) {
    return Map.of(
        "ticker", ticker,
        "date", "2026-01-02",
        "type", "BUY",
        "quantity", "1",
        "price", "10",
        "fees", "0");
  }

  @Vetoed
  private static final class TestDevDataBootstrap extends DevDataBootstrap {
    private final Map<String, List<Map<String, Object>>> seedProfiles = new HashMap<>();
    private final Map<Long, Long> credentialCounts = new HashMap<>();
    private final List<Long> deletedUserIds = new ArrayList<>();
    private final List<String> insertCalls = new ArrayList<>();
    private final List<AppUser> refreshedUsers = new ArrayList<>();
    private final List<AuthCredential> persistedCredentials = new ArrayList<>();
    private final List<com.stocktracker.domain.PortfolioTransaction> persistedTransactions =
        new ArrayList<>();
    private Set<String> bootstrappedSymbols = Set.of();
    private AppUser seedUser;
    private int seedPortfolioCalls;
    private int verifiedUserCalls;
    private final Map<Long, Long> transactionCounts = new HashMap<>();

    @Override
    AppUser ensureLegacySeedUser() {
      if (seedUser != null) {
        return seedUser;
      }
      return super.ensureLegacySeedUser();
    }

    @Override
    void ensureSeedPortfolio(AppUser user) throws Exception {
      seedPortfolioCalls++;
      super.ensureSeedPortfolio(user);
    }

    @Override
    void ensureVerifiedUser(String email, String password) {
      verifiedUserCalls++;
      super.ensureVerifiedUser(email, password);
    }

    @Override
    Set<String> insertTransactions(Long userId, String profile) throws Exception {
      insertCalls.add(userId + ":" + profile);
      return super.insertTransactions(userId, profile);
    }

    @Override
    public void refreshDemoUserPortfolio(AppUser user) throws Exception {
      refreshedUsers.add(user);
      super.refreshDemoUserPortfolio(user);
    }

    @Override
    void bootstrapSeededMarketData(Set<String> symbols) {
      bootstrappedSymbols = symbols;
      super.bootstrapSeededMarketData(symbols);
    }

    @Override
    long credentialCount(Long userId) {
      return credentialCounts.getOrDefault(userId, 1L);
    }

    @Override
    long transactionCount(Long userId) {
      return transactionCounts.getOrDefault(userId, 0L);
    }

    @Override
    void deleteTransactions(Long userId) {
      deletedUserIds.add(userId);
    }

    @Override
    AuthCredential newCredential() {
      return new AuthCredential();
    }

    @Override
    String hashPassword(String password) {
      return "hash:" + password;
    }

    @Override
    void persistCredential(AuthCredential credential) {
      persistedCredentials.add(credential);
    }

    @Override
    List<Map<String, Object>> loadDemoTransactions(String profile) {
      return superLoadDemoTransactions(profile);
    }

    private List<Map<String, Object>> superLoadDemoTransactions(String profile) {
      var resolvedProfile = profile == null || profile.isBlank() ? "seed" : profile.trim();
      var rows = seedProfiles.get(resolvedProfile);
      if (rows == null) {
        rows = seedProfiles.get("seed");
      }
      if (rows == null) {
        throw new IllegalStateException("Missing default demo transaction seed profile");
      }
      return rows;
    }
  }
}
