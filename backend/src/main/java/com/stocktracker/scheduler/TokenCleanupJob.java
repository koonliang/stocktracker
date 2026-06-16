package com.stocktracker.scheduler;

import com.stocktracker.persistence.VerificationTokenRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/** Periodically purges expired or consumed verification/reset tokens so the table stays small. */
@ApplicationScoped
public class TokenCleanupJob {
  private static final Logger LOG = Logger.getLogger(TokenCleanupJob.class);

  @Inject VerificationTokenRepository tokens;

  @Scheduled(every = "1h")
  @Transactional
  public void purge() {
    long removed = tokens.deleteExpiredOrConsumed(LocalDateTime.now());
    if (removed > 0) {
      LOG.infof("event=token_cleanup removed=%d", removed);
    }
  }
}
