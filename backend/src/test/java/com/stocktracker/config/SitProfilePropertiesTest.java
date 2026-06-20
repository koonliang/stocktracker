package com.stocktracker.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SitProfilePropertiesTest {

  @Test
  void applicationPropertiesDefinesSitProfileDefaults() throws IOException {
    String properties = readApplicationProperties();

    assertTrue(properties.contains("%sit.stocktracker.auth.mode=${SIT_AUTH_MODE:dev}"));
    assertTrue(
        properties.contains(
            "%sit.stocktracker.dev-bootstrap.enabled=${SIT_DEV_BOOTSTRAP_ENABLED:true}"));
    assertTrue(properties.contains("%sit.quarkus.scheduler.enabled=${SIT_SCHEDULER_ENABLED:true}"));
  }

  @Test
  void applicationPropertiesStillUsesEnvironmentJdbcUrl() throws IOException {
    String properties = readApplicationProperties();

    assertTrue(
        properties.contains(
            "quarkus.datasource.jdbc.url=${QUARKUS_DATASOURCE_JDBC_URL:jdbc:mysql://localhost:3306/stocktracker_dev}"));
    assertFalse(properties.contains("SIT_DB_HOST"));
    assertFalse(properties.contains("SIT_DB_PORT"));
    assertFalse(properties.contains("SIT_DB_NAME"));
  }

  private String readApplicationProperties() throws IOException {
    Path path = Path.of("src/main/resources/application.properties");
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}
