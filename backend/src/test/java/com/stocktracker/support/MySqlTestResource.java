package com.stocktracker.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.MySQLContainer;

public class MySqlTestResource implements QuarkusTestResourceLifecycleManager {
  private MySQLContainer<?> mysql;

  @Override
  public Map<String, String> start() {
    mysql =
        new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("stocktracker")
            .withUsername("stocktracker")
            .withPassword("stocktracker");
    mysql.start();

    return Map.of(
        "quarkus.datasource.devservices.enabled", "false",
        "quarkus.datasource.jdbc.url", disableSsl(mysql.getJdbcUrl()),
        "quarkus.datasource.username", mysql.getUsername(),
        "quarkus.datasource.password", mysql.getPassword());
  }

  @Override
  public void stop() {
    if (mysql != null) {
      mysql.stop();
    }
  }

  private String disableSsl(String jdbcUrl) {
    return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslMode=DISABLED";
  }
}
