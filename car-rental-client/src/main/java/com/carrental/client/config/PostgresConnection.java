package com.carrental.client.config;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Converts a provider URI without leaving credentials in the JDBC URL or error messages. */
public final class PostgresConnection {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private PostgresConnection(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl; this.username = username; this.password = password;
    }
    public String jdbcUrl() { return jdbcUrl; }
    public String username() { return username; }
    public String password() { return password; }
    @Override public String toString() { return "PostgresConnection[redacted]"; }

    public static PostgresConnection parse(String value, boolean allowInsecureLocal) {
        try {
            if (value == null || value.isBlank()) throw new IllegalArgumentException();
            URI uri = URI.create(value.startsWith("jdbc:") ? value.substring(5) : value);
            if (!("postgres".equals(uri.getScheme()) || "postgresql".equals(uri.getScheme())) ||
                uri.getHost() == null || uri.getFragment() != null || uri.getRawPath() == null ||
                !uri.getRawPath().matches("/[^/]+")) throw new IllegalArgumentException();
            String username = null, password = null;
            if (uri.getRawUserInfo() != null) {
                String[] credentials = uri.getRawUserInfo().split(":", 2);
                username = decode(credentials[0]);
                if (credentials.length == 2) password = decode(credentials[1]);
            }
            Map<String, String> parameters = new LinkedHashMap<>();
            if (uri.getRawQuery() != null) for (String entry : uri.getRawQuery().split("&")) {
                String[] pair = entry.split("=", 2);
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String content = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                if (key.equals("user")) { if (username != null) throw new IllegalArgumentException(); username = content; }
                else if (key.equals("password")) { if (password != null) throw new IllegalArgumentException(); password = content; }
                else {
                    if (key.equals("channel_binding")) key = "channelBinding";
                    if (!java.util.Set.of("sslmode", "channelBinding", "connectTimeout", "socketTimeout", "ApplicationName").contains(key)
                        || parameters.putIfAbsent(key, content) != null) throw new IllegalArgumentException();
                }
            }
            if (username == null || username.isBlank() || password == null || password.isBlank()) throw new IllegalArgumentException();
            String ssl = parameters.getOrDefault("sslmode", "verify-full");
            boolean loopback = java.util.Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost());
            if (!java.util.Set.of("require", "verify-ca", "verify-full").contains(ssl) &&
                !(ssl.equals("disable") && loopback && allowInsecureLocal)) throw new IllegalArgumentException();
            parameters.put("sslmode", ssl);
            parameters.putIfAbsent("connectTimeout", "10");
            parameters.putIfAbsent("socketTimeout", "30");
            String query = parameters.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
            String host = uri.getHost();
            String authority = host + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
            return new PostgresConnection("jdbc:postgresql://" + authority + uri.getRawPath() + "?" + query, username, password);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid PostgreSQL configuration; supply a complete DATABASE_URL with TLS (local test opt-out is loopback only).");
        }
    }
    private static String decode(String value) { return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
