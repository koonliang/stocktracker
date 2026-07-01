package com.stocktracker.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.MySQLContainer;

public class MySqlTestResource implements QuarkusTestResourceLifecycleManager {
  private static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("stocktracker")
          .withUsername("stocktracker")
          .withPassword("stocktracker");

  @Override
  public Map<String, String> start() {
    if (!MYSQL.isRunning()) {
      MYSQL.start();
    }

    return Map.of(
        "quarkus.datasource.devservices.enabled", "false",
        "quarkus.datasource.jdbc.url", disableSsl(MYSQL.getJdbcUrl()),
        "quarkus.datasource.username", MYSQL.getUsername(),
        "quarkus.datasource.password", MYSQL.getPassword());
  }

  @Override
  public void stop() {}

  private String disableSsl(String jdbcUrl) {
    return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslMode=DISABLED";
  }
}
