package com.demo.sso.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.demo.sso.SsoApplication;
import com.demo.sso.config.TestAuthCodeStoreConfig;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FlywayPostgresMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Test
    void baselinesExistingSchemaThenMigratesToRelease1Shape() throws Exception {
        createLegacySchemaViaHibernate();

        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .baselineOnMigrate(false)
            .baselineVersion("1")
            .locations("classpath:db/migration")
            .load();

        flyway.baseline();
        flyway.migrate();

        assertCurrentAppBootsAgainstMigratedSchema();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            assertColumn(connection, "provider", "NO");
            assertColumn(connection, "provider_user_id", "NO");
            assertColumn(connection, "last_login_flow", "YES");
            assertFalse(hasUniqueConstraintOnColumn(connection.getMetaData(), "users", "email"));
            assertTrue(hasUniqueConstraintOnColumns(connection.getMetaData(), "users", "provider", "provider_user_id"));
        }
    }

    private static void createLegacySchemaViaHibernate() {
        Configuration configuration = new Configuration()
            .addAnnotatedClass(LegacyGoogleUser.class)
            .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
            .setProperty("hibernate.connection.url", postgres.getJdbcUrl())
            .setProperty("hibernate.connection.username", postgres.getUsername())
            .setProperty("hibernate.connection.password", postgres.getPassword())
            .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .setProperty("hibernate.hbm2ddl.auto", "update")
            .setProperty("hibernate.show_sql", "false");

        try (var sessionFactory = configuration.buildSessionFactory()) {
            // Building the session factory with hbm2ddl=update materializes the legacy schema.
        }
    }

    private static void assertCurrentAppBootsAgainstMigratedSchema() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                SsoApplication.class,
                TestAuthCodeStoreConfig.class)
                .web(WebApplicationType.NONE)
                .run(
                    "--spring.profiles.active=test",
                    "--spring.datasource.url=" + postgres.getJdbcUrl(),
                    "--spring.datasource.driver-class-name=org.postgresql.Driver",
                    "--spring.datasource.username=" + postgres.getUsername(),
                    "--spring.datasource.password=" + postgres.getPassword(),
                    "--spring.jpa.hibernate.ddl-auto=validate",
                    "--spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                    "--spring.jpa.show-sql=false",
                    "--spring.flyway.enabled=true",
                    "--spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                    "--GOOGLE_CLIENT_ID=test-client-id",
                    "--GOOGLE_CLIENT_SECRET=test-client-secret",
                    "--JWT_SECRET=test-secret-key-that-is-at-least-32-characters-long-for-hmac",
                    "--app.google-client-id=test-client-id")) {
            assertTrue(context.isActive());
            try (Connection connection = context.getBean(javax.sql.DataSource.class).getConnection()) {
                assertTrue(connection.getMetaData().getURL().startsWith("jdbc:postgresql://"));
            }
        }
    }

    @Entity
    @Table(name = "users")
    static class LegacyGoogleUser {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "google_id", unique = true, nullable = false)
        private String googleId;

        @Column(unique = true, nullable = false)
        private String email;

        private String name;

        @Column(name = "picture_url")
        private String pictureUrl;

        @Column(name = "login_method", length = 20)
        private String loginMethod;

        @Column(name = "created_at")
        private Instant createdAt;

        @Column(name = "last_login_at")
        private Instant lastLoginAt;
    }

    private static void assertColumn(Connection connection, String columnName, String expectedNullable) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "users", columnName)) {
            assertTrue(columns.next(), () -> "Expected column to exist: " + columnName);
            assertEquals(expectedNullable, columns.getString("IS_NULLABLE"));
        }
    }

    private static boolean hasUniqueConstraintOnColumn(DatabaseMetaData metaData, String tableName, String columnName)
            throws Exception {
        return hasUniqueConstraintOnColumns(metaData, tableName, columnName);
    }

    private static boolean hasUniqueConstraintOnColumns(
            DatabaseMetaData metaData,
            String tableName,
            String... columnNames) throws Exception {
        java.util.Set<String> expected = java.util.Set.of(columnNames);
        java.util.Map<String, java.util.Set<String>> uniqueIndexes = new java.util.HashMap<>();
        try (ResultSet indexInfo = metaData.getIndexInfo(null, null, tableName, true, false)) {
            while (indexInfo.next()) {
                String indexName = indexInfo.getString("INDEX_NAME");
                String columnName = indexInfo.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    uniqueIndexes
                        .computeIfAbsent(indexName, ignored -> new java.util.HashSet<>())
                        .add(columnName.toLowerCase());
                }
            }
        }

        return uniqueIndexes.values().stream().anyMatch(columns -> columns.equals(expected));
    }
}