package com.carrental.client.security;

import com.carrental.client.model.Personne;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionUsers {
    private final CsrfTokenRepository csrf;
    public SessionUsers(CsrfTokenRepository csrf) { this.csrf = csrf; }

    public static Personne withoutPassword(Personne person) {
        return new Personne(person.getId(), person.getCin(), person.getNom(), person.getPrenom(), person.getNumero(), person.getNpermis(), person.getNbloc(), person.getLogin(), null, person.getRole());
    }
    public static Long id(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof RentalPrincipal user) return user.getId();
        if (auth.getPrincipal() instanceof RentalOidcUser user) return user.getPersonneId();
        return null;
    }
    public void loginGuest(Personne person, HttpServletRequest request, HttpServletResponse response) {
        var principal = new RentalPrincipal(person);
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        auth.eraseCredentials();
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth, request, response);
        new CsrfAuthenticationStrategy(csrf).onAuthentication(auth, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        request.getSession().setAttribute("user", withoutPassword(person));
        request.getSession().setAttribute("demoVisitor", true);
    }
}
