package com.stocktracker.api;

import static io.restassured.RestAssured.given;

import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The {@code /api/auth/*} sliding-window rate limiter throttles uniformly across endpoints by IP
 * (FR-020/SC-006): once the per-IP budget is spent on one endpoint, sibling auth endpoints are
 * throttled too. A dedicated low limit is set via {@link RateLimitProfile} (the default test
 * profile raises the limit so other suites are unaffected).
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestProfile(AuthRateLimitTest.RateLimitProfile.class)
class AuthRateLimitTest {
  private static final int MAX_ATTEMPTS = 5;

  public static class RateLimitProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "stocktracker.auth.rate-limit.max-attempts",
          String.valueOf(MAX_ATTEMPTS),
          "stocktracker.auth.rate-limit.window-seconds",
          "60");
    }
  }

  private int post(String path, Map<String, ?> body) {
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(path)
        .then()
        .extract()
        .statusCode();
  }

  @Test
  void throttlesPerIpAcrossAuthEndpoints() {
    var body = Map.of("email", "ratelimit@example.com", "password", "Passw0rd!");

    // The first MAX_ATTEMPTS login attempts are processed (never 429).
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      Assertions.assertNotEquals(429, post("/api/auth/login", body), "attempt " + i);
    }

    // The next login from the same IP is throttled.
    Assertions.assertEquals(429, post("/api/auth/login", body));

    // The limit is per-IP, not per-endpoint: signup and forgot-password are throttled too.
    Assertions.assertEquals(429, post("/api/auth/signup", body));
    Assertions.assertEquals(
        429, post("/api/auth/forgot-password", Map.of("email", "x@example.com")));
  }
}
