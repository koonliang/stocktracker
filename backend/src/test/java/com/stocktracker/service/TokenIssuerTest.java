package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.AppUser;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class TokenIssuerTest {
  private TokenIssuer service;

  @BeforeEach
  void setUp() {
    service = new TokenIssuer();
    service.ttlSeconds = 3600;
    service.issuer = "stocktracker-dev";
  }

  @Test
  void issueBuildsJwtWithExpectedClaims() {
    var user = user();
    var builder = Mockito.mock(JwtClaimsBuilder.class);
    chain(builder);
    when(builder.sign()).thenReturn("jwt");

    try (MockedStatic<Jwt> jwt = mockStatic(Jwt.class)) {
      jwt.when(() -> Jwt.issuer("stocktracker-dev")).thenReturn(builder);

      var token = service.issue(user);

      assertEquals("jwt", token);
      verify(builder).subject("5");
      verify(builder).upn("user@example.com");
      verify(builder).claim(eq("email"), eq("user@example.com"));
      verify(builder).groups(Set.of("user"));
      verify(builder).expiresIn(Duration.ofSeconds(3600));
    }
  }

  @Test
  void issueBumpsIssuedAtMillisPastSessionInvalidBefore() {
    var user = user();
    user.sessionsInvalidBeforeMs = Long.MAX_VALUE - 10;
    var builder = Mockito.mock(JwtClaimsBuilder.class);
    chain(builder);
    when(builder.sign()).thenReturn("jwt");

    try (MockedStatic<Jwt> jwt = mockStatic(Jwt.class)) {
      jwt.when(() -> Jwt.issuer("stocktracker-dev")).thenReturn(builder);

      service.issue(user);

      verify(builder).claim("st_iat_ms", Long.MAX_VALUE - 9);
    }
  }

  private void chain(JwtClaimsBuilder builder) {
    when(builder.subject(any())).thenReturn(builder);
    when(builder.upn(any())).thenReturn(builder);
    when(builder.claim(Mockito.anyString(), any())).thenReturn(builder);
    when(builder.groups(Mockito.<Set<String>>any())).thenReturn(builder);
    when(builder.expiresIn(any(Duration.class))).thenReturn(builder);
  }

  private AppUser user() {
    var user = new AppUser();
    user.id = 5L;
    user.email = "user@example.com";
    return user;
  }
}
