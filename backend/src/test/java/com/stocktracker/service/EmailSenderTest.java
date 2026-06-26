package com.stocktracker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.security.AuthMode;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class EmailSenderTest {
  private final AuthMode authMode = Mockito.mock(AuthMode.class);
  private final Mailer mailer = Mockito.mock(Mailer.class);
  private EmailSender service;

  @BeforeEach
  void setUp() {
    service = new EmailSender();
    service.authMode = authMode;
    service.mailer = mailer;
  }

  @Test
  void sendVerificationUsesDevSinkInDevMode() {
    when(authMode.isDev()).thenReturn(true);

    service.sendVerification("user@example.com", "token");

    verify(mailer, never()).send(any(Mail[].class));
  }

  @Test
  void sendPasswordResetSendsMailerMessageOutsideDev() {
    when(authMode.isDev()).thenReturn(false);
    var mail = new Mail();
    try (MockedStatic<Mail> mails = mockStatic(Mail.class)) {
      mails.when(() -> Mail.withText("user@example.com", "Reset your StockTracker password",
              "Reset your StockTracker password using this token: token"))
          .thenReturn(mail);

      service.sendPasswordReset("user@example.com", "token");

      verify(mailer).send(mail);
    }
  }
}
