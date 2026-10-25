package com.carrental.client.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@Profile("persistent")
public class PersistentDatabaseConfig {
    private static PostgresConnection connection(Environment env, String key) {
        return PostgresConnection.parse(env.getProperty(key), env.getProperty("app.database.allow-insecure-local", Boolean.class, false));
    }
    @Bean(initMethod = "migrate")
    Flyway persistentMigrations(Environment env) {
        if (env.acceptsProfiles(Profiles.of("demo"))) throw new IllegalStateException("Activate persistent alone; the demo profile resets its database.");
        PostgresConnection db = connection(env, env.getProperty("DATABASE_URL_UNPOOLED", "").isBlank() ? "DATABASE_URL" : "DATABASE_URL_UNPOOLED");
        return Flyway.configure().dataSource(db.jdbcUrl(), db.username(), db.password())
            .locations("classpath:db/migration/postgresql").cleanDisabled(true).baselineOnMigrate(false).load();
    }
    @Bean(destroyMethod = "close")
    @DependsOn("persistentMigrations")
    HikariDataSource dataSource(Environment env) {
        PostgresConnection db = connection(env, "DATABASE_URL");
        HikariConfig pool = new HikariConfig();
        pool.setJdbcUrl(db.jdbcUrl()); pool.setUsername(db.username()); pool.setPassword(db.password());
        pool.setDriverClassName("org.postgresql.Driver");
        pool.setPoolName("loc-auto"); pool.setMaximumPoolSize(3); pool.setMinimumIdle(0);
        pool.setConnectionTimeout(15000); pool.setValidationTimeout(5000); pool.setIdleTimeout(60000);
        pool.setMaxLifetime(240000); pool.setKeepaliveTime(0);
        return new HikariDataSource(pool);
    }
}
