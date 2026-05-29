package com.stocktracker.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * Temporary diagnostic for the Lambda "No virtual channel available" failure.
 *
 * <p>Two StartupEvent observers bracket all other startup work (CDI invokes
 * lower @Priority first). If BEGIN logs but COMPLETE does not, some startup
 * bean in between threw and aborted boot. If neither logs, the Quarkus
 * application never started at all. Remove once the boot issue is resolved.
 */
@ApplicationScoped
public class StartupProbe {
  private static final Logger LOG = Logger.getLogger(StartupProbe.class);

  void onStartBegin(@Observes @Priority(Integer.MIN_VALUE) StartupEvent ignored) {
    LOG.info("=== StockTracker StartupEvent BEGIN ===");
  }

  void onStartComplete(@Observes @Priority(Integer.MAX_VALUE) StartupEvent ignored) {
    LOG.info("=== StockTracker StartupEvent COMPLETE — HTTP layer should be ready ===");
  }
}
