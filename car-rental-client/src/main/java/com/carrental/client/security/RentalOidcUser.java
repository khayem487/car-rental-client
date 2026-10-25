package com.carrental.client.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class RentalOidcUser implements OidcUser, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final OidcUser delegate;
    private final Long personneId;
    public RentalOidcUser(OidcUser delegate, Long personneId) { this.delegate = delegate; this.personneId = personneId; }
    public Long getPersonneId() { return personneId; }
    @Override public String getName() { return delegate.getName(); }
    @Override public Map<String,Object> getAttributes() { return delegate.getAttributes(); }
    @Override public Map<String,Object> getClaims() { return delegate.getClaims(); }
    @Override public OidcIdToken getIdToken() { return delegate.getIdToken(); }
    @Override public OidcUserInfo getUserInfo() { return delegate.getUserInfo(); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_CLIENT"), new SimpleGrantedAuthority("OIDC_USER")); }
}
