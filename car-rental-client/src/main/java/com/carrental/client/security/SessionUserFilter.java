package com.carrental.client.security;

import com.carrental.client.dao.PersonneDAO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** The legacy view/session DTO always comes from authenticated identity, never form data. */
public final class SessionUserFilter extends OncePerRequestFilter {
    private final PersonneDAO personnes;
    public SessionUserFilter(PersonneDAO personnes) { this.personnes = personnes; }
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
            request.getSession().setAttribute("user", SessionUsers.withoutPassword(user.get()));
        }
        chain.doFilter(request, response);
    }
}
