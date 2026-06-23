package com.stocktracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.api.ApiException;
import com.stocktracker.config.NonProdAuthConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GoogleAuthClient {
  private static final Logger LOG = Logger.getLogger(GoogleAuthClient.class);
  private static final URI TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
  private static final URI USERINFO_URI =
      URI.create("https://openidconnect.googleapis.com/v1/userinfo");
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  @Inject NonProdAuthConfig config;
  @Inject ObjectMapper objectMapper;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  public ProviderProfile exchange(String code, String redirectUri) {
    config.validateProviderCredentials("google");
    var expectedRedirectUri = config.requireRedirectUri(redirectUri);
    if (code == null || code.isBlank()) {
      throw new ApiException(Status.BAD_REQUEST, "AUTH_FAILED", "Unable to complete sign-in.");
    }

    var accessToken = exchangeCodeForAccessToken(code.trim(), expectedRedirectUri);
    return fetchUserProfile(accessToken);
  }

  private String exchangeCodeForAccessToken(String code, String redirectUri) {
    var body =
        formBody(
            new LinkedHashMap<>() {
              {
                put("client_id", config.googleClientId());
                put("client_secret", config.googleClientSecret());
                put("code", code);
                put("grant_type", "authorization_code");
                put("redirect_uri", redirectUri);
              }
            });

    var request =
        HttpRequest.newBuilder(TOKEN_URI)
            .timeout(TIMEOUT)
            .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    var response = send(request, "google_token_exchange");
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      LOG.warnf("event=google_token_exchange_failed status=%d", response.statusCode());
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
    }

    var json = parseJson(response.body(), "google_token_exchange");
    var accessToken = text(json, "access_token");
    if (accessToken == null || accessToken.isBlank()) {
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
    }
    return accessToken;
  }

  private ProviderProfile fetchUserProfile(String accessToken) {
    var request =
        HttpRequest.newBuilder(USERINFO_URI)
            .timeout(TIMEOUT)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .GET()
            .build();

    var response = send(request, "google_userinfo");
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      LOG.warnf("event=google_userinfo_failed status=%d", response.statusCode());
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
    }

    var json = parseJson(response.body(), "google_userinfo");
    var subject = text(json, "sub");
    if (subject == null || subject.isBlank()) {
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
    }

    return new ProviderProfile(
        subject, blankToNull(text(json, "email")), json.path("email_verified").asBoolean(false));
  }

  private HttpResponse<String> send(HttpRequest request, String event) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException exception) {
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      LOG.warnf(exception, "event=%s_failed", event);
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
    }
  }

  private JsonNode parseJson(String body, String event) {
    try {
      return objectMapper.readTree(body);
    } catch (IOException exception) {
      LOG.warnf(exception, "event=%s_invalid_json", event);
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
    }
  }

  private String formBody(LinkedHashMap<String, String> fields) {
    return fields.entrySet().stream()
        .map(
            entry ->
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }

  private String text(JsonNode node, String field) {
    var value = node.path(field);
    return value.isTextual() ? value.asText() : null;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  public record ProviderProfile(String subject, String email, boolean emailVerified) {}
}
