package com.stocktracker.migration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public class MigrationHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String jdbcUrl = System.getenv("QUARKUS_DATASOURCE_JDBC_URL");
        String username = System.getenv("QUARKUS_DATASOURCE_USERNAME");
        String password = System.getenv("QUARKUS_DATASOURCE_PASSWORD");

        Flyway flyway =
                Flyway.configure()
                        .dataSource(jdbcUrl, username, password)
                        .locations("classpath:db/migration")
                        .load();

        var result = flyway.migrate();

        if (!result.success) {
            throw new RuntimeException(
                    "Flyway migration failed: " + result.migrationsExecuted + " executed before failure");
        }

        return Map.of(
                "status", "OK",
                "migrationsExecuted", result.migrationsExecuted,
                "targetSchemaVersion",
                        result.targetSchemaVersion != null ? result.targetSchemaVersion : "none");
    }
}
