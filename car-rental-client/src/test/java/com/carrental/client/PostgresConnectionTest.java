package com.carrental.client;

import com.carrental.client.config.PostgresConnection;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PostgresConnectionTest {
    @Test void providerCredentialsAreDecodedSeparatelyAndTlsIsRequired() {
        var connection = PostgresConnection.parse("postgres://alice%2Bdemo:p%40ss%3A%2B@pool.example.test/rental?sslmode=require&channel_binding=require", false);
        assertThat(connection.username()).isEqualTo("alice+demo");
        assertThat(connection.password()).isEqualTo("p@ss:+");
        assertThat(connection.jdbcUrl()).startsWith("jdbc:postgresql://pool.example.test/rental?")
            .contains("sslmode=require", "channelBinding=require").doesNotContain("alice", "p%40ss", "password");
        assertThat(connection.toString()).isEqualTo("PostgresConnection[redacted]");
        assertThat(PostgresConnection.parse("jdbc:postgresql://host.test/rental?user=alice&password=fake", false).jdbcUrl())
            .contains("sslmode=verify-full").doesNotContain("alice", "fake");
    }
    @Test void insecureAndUnexpectedOptionsAreRejectedWithoutEchoingTheSecret() {
        for (String url : new String[]{"postgres://alice:VERY_SECRET@remote.test/rental?sslmode=disable",
            "postgres://alice:VERY_SECRET@localhost/rental?sslmode=disable",
            "postgres://alice:VERY_SECRET@remote.test/rental?sslfactory=bad.Class",
            "not a uri VERY_SECRET"}) {
            assertThatThrownBy(() -> PostgresConnection.parse(url, false)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("VERY_SECRET").hasNoCause();
        }
        assertThatThrownBy(() -> PostgresConnection.parse("postgres://alice:fake@remote.test/rental?sslmode=disable", true))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(PostgresConnection.parse("postgres://alice:fake@127.0.0.1:55432/rental?sslmode=disable", true).jdbcUrl())
            .contains("sslmode=disable");
    }
}
