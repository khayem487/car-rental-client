package com.carrental.client.security;

import com.carrental.client.model.Personne;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class RentalPrincipal implements UserDetails, CredentialsContainer {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String login;
    private String password;
    public RentalPrincipal(Personne personne) { id = personne.getId(); login = personne.getLogin(); password = personne.getPasswd(); }
    public Long getId() { return id; }
    @Override public String getUsername() { return login; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_CLIENT")); }
    @Override public void eraseCredentials() { password = null; }
}
