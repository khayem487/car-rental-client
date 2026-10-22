package com.carrental.client.security;

import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.model.Personne;
import com.carrental.client.service.PersonneService;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OidcAccounts {
    private final JdbcTemplate jdbc;
    private final PersonneService personnes;
    private final PersonneDAO dao;
    private final TransactionTemplate transactions;
    public OidcAccounts(JdbcTemplate jdbc, PersonneService personnes, PersonneDAO dao, PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.personnes = personnes; this.dao = dao; this.transactions = new TransactionTemplate(manager);
    }
    private Long find(String subject) {
        List<Long> ids = jdbc.query("SELECT personne_id FROM oidc_identity WHERE provider='google' AND subject=?", (row, index) -> row.getLong(1), subject);
        return ids.isEmpty() ? null : ids.get(0);
    }
    public Long resolve(OidcUser identity) {
        String subject = identity.getSubject();
        String email = identity.getEmail();
        String issuer = identity.getIssuer() == null ? "" : identity.getIssuer().toString();
        if (!issuer.equals("https://accounts.google.com") || !Boolean.TRUE.equals(identity.getEmailVerified()) ||
            subject == null || subject.isBlank() || subject.length() > 255 || email == null || email.length() > 320 || !email.matches("[^\\s@]+@[^\\s@]+")) throw rejected();
        try {
            Long id = transactions.execute(status -> {
                Long existing = find(subject);
                if (existing != null) return existing;
                String suffix = UUID.randomUUID().toString().replace("-", "");
                Personne person = new Personne();
                person.setLogin("google." + suffix);
                person.setPrenom(name(identity.getGivenName(), "Compte"));
                person.setNom(name(identity.getFamilyName(), "Google"));
                person.setPasswd(UUID.randomUUID().toString());
                person.setCin("OIDC-" + suffix.substring(0, 12));
                person.setNpermis("NON-VERIFIE");
                person.setNumero("NON-VERIFIE");
                Long created = personnes.enregistrer(person);
                jdbc.update("INSERT INTO oidc_identity(provider, subject, personne_id, email) VALUES ('google', ?, ?, ?)", subject, created, email);
                return created;
            });
            if (id == null || dao.findById(id).filter(person -> "CLIENT".equals(person.getRole())).isEmpty()) throw rejected();
            return id;
        } catch (DataIntegrityViolationException race) {
            // A simultaneous first login can win the unique subject. Its losing transaction rolls back fully.
            Long existing = find(subject);
            if (existing != null && dao.findById(existing).filter(person -> "CLIENT".equals(person.getRole())).isPresent()) return existing;
            throw rejected();
        }
    }
    private static String name(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String clean = value.replaceAll("[\\p{Cntrl}]", "").strip();
        return clean.isEmpty() ? fallback : clean.substring(0, Math.min(50, clean.length()));
    }
    private static OAuth2AuthenticationException rejected() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_identity"), "L’identité Google n’a pas pu être vérifiée.");
    }
}
