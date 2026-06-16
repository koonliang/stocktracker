package com.stocktracker.scheduler;

import com.stocktracker.persistence.FxRateRepository;
import com.stocktracker.service.provider.FxRateProvider;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Caches daily FX rates for the currencies in use (distinct instrument currencies + every user's
 * base currency). Runs daily, once at startup, and on demand when a new-currency instrument is
 * added. On a provider failure the prior rows remain (CurrencyService serves the last-known rate
 * marked stale).
 */
@ApplicationScoped
public class FxRefreshJob {
  private static final Logger LOG = Logger.getLogger(FxRefreshJob.class);

  @Inject FxRateProvider fxRateProvider;
  @Inject FxRateRepository fxRates;
  @Inject EntityManager entityManager;
  @Inject Clock clock;

  @ConfigProperty(name = "stocktracker.base-currency.default", defaultValue = "USD")
  String defaultBaseCurrency;

  @ConfigProperty(name = "stocktracker.dev-bootstrap.enabled", defaultValue = "true")
  boolean enabled;

  /** Seed FX rates at startup (after reference data is bootstrapped) so dev/tests have rates. */
  void onStart(@Observes @Priority(20) StartupEvent ignored) {
    if (enabled) {
      refresh();
    }
  }

  @Scheduled(cron = "0 0 1 * * ?")
  void scheduled() {
    refresh();
  }

  @Transactional
  public void refresh() {
    var currencies = currenciesInUse();
    if (currencies.size() < 2) {
      return; // nothing to convert between
    }
    var today = LocalDate.now(clock);
    for (var base : currencies) {
      var quotes = new LinkedHashSet<>(currencies);
      quotes.remove(base);
      var rates = fxRateProvider.dailyRates(base, quotes, today);
      for (var rate : rates) {
        var row = fxRates.findOrNew(rate.base(), rate.quote(), rate.date());
        row.rate = rate.rate();
        row.source = "fx-provider";
        row.stale = false;
        fxRates.persist(row); // fully populated before insert is scheduled
      }
    }
  }

  private Set<String> currenciesInUse() {
    var currencies = new LinkedHashSet<String>();
    currencies.add(defaultBaseCurrency.toUpperCase());
    currencies.addAll(
        entityManager
            .createQuery("select distinct i.currency from Instrument i", String.class)
            .getResultList());
    currencies.addAll(
        entityManager
            .createQuery("select distinct u.baseCurrency from AppUser u", String.class)
            .getResultList());
    return currencies;
  }
}
