package com.stocktracker.e2e.support;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Fetches the latest verification/reset token from the dev-only endpoint (`GET
 * /api/dev/auth/latest-token`, FR-T02) so the journey can drive verify/reset without a live inbox.
 * The backend base URL comes from {@code -De2e.backendBaseUrl} (default {@code
 * http://localhost:8080}). The response is tiny JSON; the token is extracted with a regex to avoid
 * pulling a JSON dependency into the e2e module.
 */
public final class DevTokenClient {
  private static final String DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";
  private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final String backendBaseUrl;

  public DevTokenClient() {
    this.backendBaseUrl = System.getProperty("e2e.backendBaseUrl", DEFAULT_BACKEND_BASE_URL);
  }

  public String latestVerificationToken(String email) {
    return fetchToken(email, "EMAIL_VERIFICATION");
  }

  public String latestResetToken(String email) {
    return fetchToken(email, "PASSWORD_RESET");
  }

  private String fetchToken(String email, String purpose) {
    var uri =
        URI.create(
            backendBaseUrl
                + "/api/dev/auth/latest-token?email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&purpose="
                + purpose);
    var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "No %s token for %s (HTTP %d)".formatted(purpose, email, response.statusCode()));
      }
      var matcher = TOKEN.matcher(response.body());
      if (!matcher.find()) {
        throw new IllegalStateException("Token field missing in dev-token response");
      }
      return matcher.group(1);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Failed to reach dev-token endpoint", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted fetching dev token", exception);
    }
  }
}
