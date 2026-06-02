package com.stocktracker.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/**
 * Resolves {@code quarkus.datasource.password} from the RDS-managed Secrets Manager secret using
 * the AWS SDK. Active only when {@code DATASOURCE_PASSWORD_SECRET_ARN} is set (i.e. on Lambda);
 * otherwise it provides nothing and lower-ordinal sources (env var / application.properties
 * default) keep working for local dev and tests.
 *
 * <p>Quarkus reads this during config validation in the Lambda INIT phase. The SDK calls Secrets
 * Manager directly, which works in INIT — unlike the Parameters and Secrets Lambda Extension, whose
 * localhost cache is only available during the INVOKE phase and so returned "not ready to serve
 * traffic" here.
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

  private String fetchPassword() {
    // Credentials and region come from the Lambda environment via the SDK's default
    // provider chain. url-connection-client avoids pulling in Netty/Apache HttpClient.
    try (SecretsManagerClient client =
        SecretsManagerClient.builder().httpClient(UrlConnectionHttpClient.create()).build()) {
      String secretString =
          client
              .getSecretValue(GetSecretValueRequest.builder().secretId(SECRET_ARN).build())
              .secretString();
      return extractPassword(secretString);
    }
  }

  private String extractPassword(String secretString) {
    try {
      JsonNode password = new ObjectMapper().readTree(secretString).get("password");
      if (password == null) {
        throw new IllegalStateException("RDS-managed secret missing 'password'");
      }
      return password.asText();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse RDS-managed secret", e);
    }
  }
}
