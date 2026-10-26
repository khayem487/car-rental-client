package com.carrental.client.security;

import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.model.Personne;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** The legacy view/session DTO always comes from authenticated identity, never form data. */
public final class SessionUserFilter extends OncePerRequestFilter {
    private final PersonneDAO personnes;
    public SessionUserFilter(PersonneDAO personnes) { this.personnes = personnes; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ("GET".equals(method) || "HEAD".equals(method))
            && (path.startsWith("/css/") || path.startsWith("/js/"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Long id = SessionUsers.id(SecurityContextHolder.getContext().getAuthentication());
        if (id == null) {
            var session = request.getSession(false);
            if (session != null) { session.removeAttribute("user"); session.removeAttribute("demoVisitor"); }
        } else {
            var user = personnes.findById(id).filter(person -> "CLIENT".equals(person.getRole()));
            if (user.isEmpty()) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
            var session = request.getSession();
            Personne current = user.get();
            if (!sameUser(session.getAttribute("user"), current)) {
                session.setAttribute("user", SessionUsers.withoutPassword(current));
            }
        }
        chain.doFilter(request, response);
    }
    private static boolean sameUser(Object saved, Personne current) {
        return saved instanceof Personne previous && previous.getPasswd() == null
            && Objects.equals(previous.getId(), current.getId())
            && Objects.equals(previous.getCin(), current.getCin())
            && Objects.equals(previous.getNom(), current.getNom())
            && Objects.equals(previous.getPrenom(), current.getPrenom())
            && Objects.equals(previous.getNumero(), current.getNumero())
            && Objects.equals(previous.getNpermis(), current.getNpermis())
            && previous.getNbloc() == current.getNbloc()
            && Objects.equals(previous.getLogin(), current.getLogin())
            && Objects.equals(previous.getRole(), current.getRole());
    }
}
