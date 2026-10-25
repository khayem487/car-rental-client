package com.carrental.client.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.DefaultCookieSerializer;

/** Persist security context, CSRF and OAuth authorization state across instances. */
@Configuration
@Profile("persistent")
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800, cleanupCron = "-")
public class PersistentSessionConfig {
    @Bean DefaultCookieSerializer cookieSerializer(Environment env) {
        DefaultCookieSerializer cookie = new DefaultCookieSerializer();
        cookie.setCookieName("JSESSIONID"); cookie.setUseBase64Encoding(false);
        cookie.setUseHttpOnlyCookie(true); cookie.setSameSite("Lax");
        cookie.setUseSecureCookie(env.getProperty("SESSION_COOKIE_SECURE", Boolean.class, false));
        return cookie;
    }
    @Bean ApplicationRunner removeExpiredSessions(JdbcIndexedSessionRepository sessions) {
        // One cleanup per cold start; no scheduler or keep-alive wakes a sleeping database.
        return args -> sessions.cleanUpExpiredSessions();
    }
}
