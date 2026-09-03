package com.xnelo.filearch.common.testfixtures.testresource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {
  private static final String DATABASE_NAME = "FILEARCH";
  private static final String DATABASE_USERNAME = "xnelo";
  private static final String DATABASE_PASSWORD = "buttmunch";
  private static final String DATABASE_SCHEMA = "FILEARCH";

  private static PostgreSQLContainer<?> database;

  @Override
  public Map<String, String> start() {
    if (database == null) {
      database =
          new PostgreSQLContainer<>("postgres:16")
              .withDatabaseName(DATABASE_NAME)
              .withUsername(DATABASE_USERNAME)
              .withPassword(DATABASE_PASSWORD);

      database.start();
    }

    try (Connection connection =
        DriverManager.getConnection(database.getJdbcUrl(), DATABASE_USERNAME, DATABASE_PASSWORD)) {
      Statement statement = connection.createStatement();
      statement.execute("CREATE SCHEMA IF NOT EXISTS " + DATABASE_SCHEMA);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Flyway.configure()
        .dataSource(database.getJdbcUrl(), DATABASE_USERNAME, DATABASE_PASSWORD)
        .schemas(DATABASE_SCHEMA)
        .locations("filesystem:../filearch-db-migration/migrations")
        .validateOnMigrate(true)
        .load()
        .migrate();

    return Map.of(
        "quarkus.datasource.jdbc.url",
        database.getJdbcUrl(),
        "quarkus.datasource.username",
        DATABASE_USERNAME,
        "quarkus.datasource.password",
        DATABASE_PASSWORD);
  }

  @Override
  public void stop() {
    if (database != null) {
      database.stop();
      database = null;
    }
  }
}
