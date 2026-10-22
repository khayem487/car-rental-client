package com.carrental.client.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    CommandLineRunner verifyDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) throw new SQLException("Invalid SQL connection");
                LOG.info("Database connection verified");
            } catch (SQLException failure) {
                // Driver exception messages can contain JDBC URLs or credentials.
                throw new IllegalStateException("Database connection unavailable; verify the server configuration.");
            }
        };
    }
}
