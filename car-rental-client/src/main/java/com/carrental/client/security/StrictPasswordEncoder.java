package com.carrental.client.security;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Reject BCrypt truncation explicitly, including multibyte UTF-8 and NUL input. */
public final class StrictPasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(12);

    public static boolean valid(String value) {
        return value != null && value.length() >= 8 && value.indexOf('\0') < 0
            && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override public String encode(CharSequence raw) {
        if (raw == null || !valid(raw.toString())) throw new IllegalArgumentException("Utilisez au moins 8 caractères et 72 octets UTF-8 maximum pour le mot de passe.");
        return delegate.encode(raw);
    }
    @Override public boolean matches(CharSequence raw, String encoded) {
        return raw != null && valid(raw.toString()) && encoded != null && encoded.matches("\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}") && delegate.matches(raw, encoded);
    }
    @Override public boolean upgradeEncoding(String encoded) { return delegate.upgradeEncoding(encoded); }
}
