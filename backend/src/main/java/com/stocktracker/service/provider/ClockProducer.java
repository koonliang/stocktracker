package com.stocktracker.service.provider;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/** System UTC clock; {@code @DefaultBean} so tests can substitute a fixed clock. */
@ApplicationScoped
public class ClockProducer {
  @Produces
  @DefaultBean
  @ApplicationScoped
  Clock clock() {
    return Clock.systemUTC();
  }
}
