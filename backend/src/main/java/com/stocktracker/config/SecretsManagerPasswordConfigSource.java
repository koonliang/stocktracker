package com.stocktracker.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Resolves {@code quarkus.datasource.password} from the RDS-managed Secrets Manager secret via the
 * AWS Parameters and Secrets Lambda Extension's local cache ({@code http://localhost:2773}). Active
 * only when {@code DATASOURCE_PASSWORD_SECRET_ARN} is set (i.e. on Lambda); otherwise it provides
 * nothing and lower-ordinal sources (env var / application.properties default) keep working for
 * local dev and tests.
 *
 * <p>The value is fetched once per container and cached, so a rotated password is picked up on the
 * next deploy / cold start (spec US5 AS2).
 */
public final class SecretsManagerPasswordConfigSource implements ConfigSource {

  private static final String KEY = "quarkus.datasource.password";
  private static final String SECRET_ARN = System.getenv("DATASOURCE_PASSWORD_SECRET_ARN");

  private volatile String cachedPassword;
  private volatile boolean resolved;

  @Override
  public int getOrdinal() {
    // Above environment variables (300) and application.properties (250).
    return 400;
  }

  @Override
  public String getName() {
    return "SecretsManagerPasswordConfigSource";
  }

  @Override
  public Set<String> getPropertyNames() {
    return isActive() ? Set.of(KEY) : Collections.emptySet();
  }

  @Override
  public Map<String, String> getProperties() {
    String value = getValue(KEY);
    return value == null ? Collections.emptyMap() : Map.of(KEY, value);
  }

  @Override
  public String getValue(String propertyName) {
    if (!KEY.equals(propertyName) || !isActive()) {
      return null;
    }
    if (!resolved) {
      synchronized (this) {
        if (!resolved) {
          cachedPassword = fetchPassword();
          resolved = true;
        }
      }
    }
    return cachedPassword;
  }

  private boolean isActive() {
    return SECRET_ARN != null && !SECRET_ARN.isBlank();
  }

  // A serving extension responds instantly, so a short retry only rides out the
  // brief window where the localhost port is not yet accepting connections. Use
  // 127.0.0.1 (not "localhost"): the extension binds IPv4 only, but "localhost"
  // can resolve to IPv6 ::1, where every connect is refused regardless of waiting.
  private static final Duration RETRY_BUDGET = Duration.ofSeconds(5);
  private static final Duration RETRY_DELAY = Duration.ofMillis(500);

  private String fetchPassword() {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    String token = System.getenv("AWS_SESSION_TOKEN");
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://127.0.0.1:2773/secretsmanager/get?secretId="
                        + URLEncoder.encode(SECRET_ARN, StandardCharsets.UTF_8)))
            .header("X-Aws-Parameters-Secrets-Token", token == null ? "" : token)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    long deadline = System.nanoTime() + RETRY_BUDGET.toNanos();
    int attempt = 0;
    // Keep the real reason every attempt fails (HTTP status + body, or the
    // connection exception) so a persistent failure surfaces the cause instead
    // of a generic "N attempts" message.
    String lastFailure = null;
    Throwable lastCause = null;
    while (true) {
      attempt++;
      try {
        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          return extractPassword(response.body());
        }
        lastFailure = "HTTP " + response.statusCode() + " from extension: " + response.body();
        lastCause = null;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted resolving datasource password", e);
      } catch (Exception e) {
        lastFailure = e.getClass().getSimpleName() + ": " + e.getMessage();
        lastCause = e;
      }
      if (System.nanoTime() >= deadline) {
        break;
      }
      try {
        Thread.sleep(RETRY_DELAY.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted resolving datasource password", e);
      }
    }
    throw new IllegalStateException(
        "Failed to resolve datasource password from Secrets Manager extension after "
            + attempt
            + " attempts over "
            + RETRY_BUDGET.toSeconds()
            + "s; last failure: "
            + lastFailure,
        lastCause);
  }

  private String extractPassword(String body) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode secretString = mapper.readTree(body).get("SecretString");
      if (secretString == null) {
        throw new IllegalStateException("Secret response missing 'SecretString'");
      }
      JsonNode password = mapper.readTree(secretString.asText()).get("password");
      if (password == null) {
        throw new IllegalStateException("RDS-managed secret missing 'password'");
      }
      return password.asText();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Secrets Manager extension response", e);
    }
  }
}
