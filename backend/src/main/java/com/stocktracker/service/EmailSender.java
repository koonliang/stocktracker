package com.stocktracker.service;

import com.stocktracker.domain.VerificationToken.Purpose;
import com.stocktracker.security.AuthMode;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Delivers verification/reset emails. In dev mode the raw token is logged (and exposed via the
 * dev-only token endpoint) so flows work without a live inbox; otherwise it goes out via {@code
 * quarkus-mailer}. Production uses Cognito and never exercises this sender.
 */
@ApplicationScoped
public class EmailSender {
  private static final Logger LOG = Logger.getLogger(EmailSender.class);

  @Inject AuthMode authMode;
  @Inject Mailer mailer;

  public void sendVerification(String email, String rawToken) {
    send(email, Purpose.EMAIL_VERIFICATION, "Verify your StockTracker email", rawToken);
  }

  public void sendPasswordReset(String email, String rawToken) {
    send(email, Purpose.PASSWORD_RESET, "Reset your StockTracker password", rawToken);
  }

  private void send(String email, Purpose purpose, String subject, String rawToken) {
    if (authMode.isDev()) {
      LOG.infof("event=email_dev_sink purpose=%s email=%s token=%s", purpose, email, rawToken);
      return;
    }
    mailer.send(Mail.withText(email, subject, subject + " using this token: " + rawToken));
  }
}
