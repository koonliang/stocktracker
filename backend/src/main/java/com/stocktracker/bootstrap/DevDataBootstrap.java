package com.stocktracker.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.config.NonProdAuthConfig;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DevDataBootstrap {
  private static final String LEGACY_SEED_USER_EMAIL = "seed@stocktracker.local";

  @Inject InstrumentRepository instrumentRepository;
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject AppUserRepository appUserRepository;
  @Inject ObjectMapper objectMapper;
  @Inject NonProdAuthConfig nonProdAuthConfig;

  /** Documented default dev password for the seed accounts (policy-compliant; dev-mode only). */
  private static final String SEED_USER_PASSWORD = "DevPass123!";
  // A second verified account with no data, used by the e2e per-user isolation scenario (FR-006).
  private static final String EMPTY_USER_EMAIL = "empty@stocktracker.local";
  private static final String EMPTY_USER_PASSWORD = "DevPass123!";
  private static final String DEFAULT_DEMO_SEED_PROFILE = "seed";

  @ConfigProperty(name = "stocktracker.dev-bootstrap.enabled", defaultValue = "true")
  boolean enabled;

  @Transactional
  void onStart(@Observes @Priority(2) StartupEvent ignored) throws Exception {
    if (!enabled) {
      return;
    }
    ensureLegacySeedCredential();
    // A second verified, sign-in-capable account that owns no data (e2e isolation scenario).
    ensureVerifiedUser(EMPTY_USER_EMAIL, EMPTY_USER_PASSWORD);
    deleteLegacySeedTransactions();
    if (!nonProdAuthConfig.demoUsersEnabled()) {
      return;
    }
    for (var demoUser : appUserRepository.listDemoUsers()) {
      refreshDemoUserPortfolio(demoUser);
    }
  }

  @Transactional
  public void refreshDemoUserPortfolio(AppUser user) throws Exception {
    if (user == null || user.id == null || user.accountKind != AppUser.AccountKind.DEMO) {
      return;
    }

    PortfolioTransaction.delete("userId", user.id);
    for (var row : loadDemoTransactions(user.demoSeedProfile)) {
      var symbol = row.get("ticker").toString().toUpperCase();
      if (!instrumentRepository.existsSymbol(symbol)) {
        throw new IllegalStateException(
            "Missing instrument seed data for demo transaction symbol: " + symbol);
      }
      var transaction = new PortfolioTransaction();
      transaction.userId = user.id;
      transaction.tradeDate = LocalDate.parse(row.get("date").toString());
      transaction.instrumentSymbol = symbol;
      transaction.transactionType = row.get("type").toString();
      transaction.quantity = new BigDecimal(row.get("quantity").toString());
      transaction.price = new BigDecimal(row.get("price").toString());
      transaction.fees = new BigDecimal(row.get("fees").toString());
      transaction.source = "MANUAL";
      transactionRepository.persist(transaction);
    }
  }

  private void ensureLegacySeedCredential() {
    var seedUser =
        appUserRepository
            .findByNormalizedEmail(LEGACY_SEED_USER_EMAIL)
            .orElseThrow(
                () -> new IllegalStateException("Seed user missing; V2 migration not applied"));
    if (AuthCredential.count("userId", seedUser.id) > 0) {
      return;
    }
    var credential = new AuthCredential();
    credential.userId = seedUser.id;
    credential.passwordHash = BcryptUtil.bcryptHash(SEED_USER_PASSWORD);
    credential.persist();
  }

  /** Finds or creates a verified, sign-in-capable account with the given dev password. */
  private void ensureVerifiedUser(String email, String password) {
    var user = appUserRepository.findByNormalizedEmail(email).orElse(null);
    if (user == null) {
      user = new AppUser();
      user.email = AppUser.normalizeEmail(email);
      user.status = AppUser.Status.ACTIVE;
      user.emailVerified = true;
      appUserRepository.persist(user);
    }
    if (AuthCredential.count("userId", user.id) == 0) {
      var credential = new AuthCredential();
      credential.userId = user.id;
      credential.passwordHash = BcryptUtil.bcryptHash(password);
      credential.persist();
    }
  }

  private void deleteLegacySeedTransactions() {
    appUserRepository
        .findByNormalizedEmail(LEGACY_SEED_USER_EMAIL)
        .ifPresent(user -> PortfolioTransaction.delete("userId", user.id));
  }

  private List<Map<String, Object>> loadDemoTransactions(String profile) throws Exception {
    try (InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("seed/demo-transactions.json")) {
      if (stream == null) {
        throw new IllegalStateException("Missing demo transaction seed resource");
      }
      var profiles =
          objectMapper.readValue(
              stream, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
      var resolvedProfile =
          profile == null || profile.isBlank() ? DEFAULT_DEMO_SEED_PROFILE : profile.trim();
      var rows = profiles.get(resolvedProfile);
      if (rows == null) {
        rows = profiles.get(DEFAULT_DEMO_SEED_PROFILE);
      }
      if (rows == null) {
        throw new IllegalStateException("Missing default demo transaction seed profile");
      }
      return rows;
    }
  }

}
