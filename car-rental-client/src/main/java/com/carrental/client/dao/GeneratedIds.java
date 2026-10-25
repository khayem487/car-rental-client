package com.carrental.client.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/** One connection for the insert and its ID, including Oracle's original explicit sequences. */
final class GeneratedIds {
    private GeneratedIds() { }
    static Long insert(JdbcTemplate jdbc, String table, String idColumn, String sequence, String columns, Object... values) {
        return jdbc.execute((ConnectionCallback<Long>) connection -> {
            boolean postgres = "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName());
            Long explicitId = null;
            if (!postgres) {
                try (PreparedStatement next = connection.prepareStatement("SELECT " + sequence + ".NEXTVAL FROM dual"); ResultSet rs = next.executeQuery()) {
                    if (!rs.next()) throw new SQLException("No sequence value");
                    explicitId = rs.getLong(1);
                }
            }
            String placeholders = String.join(", ", Collections.nCopies(values.length, "?"));
            String sql = "INSERT INTO " + table + " (" + (postgres ? "" : idColumn + ", ") + columns + ") VALUES (" +
                (postgres ? "" : "?, ") + placeholders + ")";
            try (PreparedStatement insert = connection.prepareStatement(sql, new String[]{idColumn})) {
                int index = 1;
                if (!postgres) insert.setLong(index++, explicitId);
                for (Object value : values) insert.setObject(index++, value);
                if (insert.executeUpdate() != 1) throw new SQLException("Insert did not create one row");
                if (!postgres) return explicitId;
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No generated identifier");
                    return keys.getLong(1);
                }
            }
        });
    }
}
