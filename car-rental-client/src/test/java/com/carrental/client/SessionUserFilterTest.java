package com.carrental.client;

import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.model.Personne;
import com.carrental.client.security.RentalPrincipal;
import com.carrental.client.security.SessionUserFilter;
import com.carrental.client.security.SessionUsers;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionUserFilterTest {
    private final PersonneDAO personnes = mock(PersonneDAO.class);
    private final SessionUserFilter filter = new SessionUserFilter(personnes);

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void staticReadsDoNotRefreshAccountButOtherRequestsStillDo() throws Exception {
        Personne person = client();
        authenticate(person);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", SessionUsers.withoutPassword(person));
        for (String method : new String[]{"GET", "HEAD"}) {
            for (String path : new String[]{"/rental/css/style.css", "/rental/js/script.js"}) {
                MockHttpServletRequest request = request(method, path, session);
                request.setContextPath("/rental");
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
            }
        }
        verifyNoInteractions(personnes);
        when(personnes.findById(42L)).thenReturn(Optional.of(person));
        for (String path : new String[]{"/profile", "/css-not-a-directory", "/js-not-a-directory"}) {
            filter.doFilter(request("GET", path, session), new MockHttpServletResponse(), (req, res) -> {});
        }
        filter.doFilter(request("POST", "/css/style.css", session), new MockHttpServletResponse(), (req, res) -> {});
        verify(personnes, times(4)).findById(42L);
    }

    @Test void unchangedProfileIsCheckedWithoutRewritingSessionAndChangedFieldsAreRefreshed() throws Exception {
        Personne person = client();
        authenticate(person);
        when(personnes.findById(42L)).thenReturn(Optional.of(person));
        MockHttpSession session = spy(new MockHttpSession());
        Personne saved = SessionUsers.withoutPassword(person);
        session.setAttribute("user", saved);
        clearInvocations(session);
        filter.doFilter(request("GET", "/profile", session), new MockHttpServletResponse(), (req, res) -> {});
        verify(session, never()).setAttribute(eq("user"), any());
        assertThat(session.getAttribute("user")).isSameAs(saved);

        person.setNom("Actualisé"); person.setNumero("NOUVEAU"); person.setNbloc(9);
        filter.doFilter(request("GET", "/profile", session), new MockHttpServletResponse(), (req, res) -> {});
        Personne refreshed = (Personne) session.getAttribute("user");
        assertThat(refreshed).isNotSameAs(saved);
        assertThat(refreshed.getNom()).isEqualTo("Actualisé");
        assertThat(refreshed.getNumero()).isEqualTo("NOUVEAU");
        assertThat(refreshed.getNbloc()).isEqualTo(9);
        assertThat(refreshed.getPasswd()).isNull();
        verify(personnes, times(2)).findById(42L);
        verify(session).setAttribute(eq("user"), any());
    }

    @Test void deletedOrRevokedAccountsStillInvalidateDocumentSessions() throws Exception {
        for (boolean deleted : new boolean[]{true, false}) {
            Personne person = client();
            authenticate(person);
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("user", SessionUsers.withoutPassword(person));
            if (!deleted) person.setRole("ADMIN");
            when(personnes.findById(42L)).thenReturn(deleted ? Optional.empty() : Optional.of(person));
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean reachedPage = new AtomicBoolean();
            filter.doFilter(request("GET", "/profile", session), response, (req, res) -> reachedPage.set(true));
            assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
            assertThat(session.isInvalid()).isTrue();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(reachedPage).isFalse();
        }
    }

    private static Personne client() {
        return new Personne(42L, "DEMO-42", "Exemple", "Client", "DEMO", "DEMO-42", 0,
            "client.test", "fictional-encoded-password", "CLIENT");
    }
    private static void authenticate(Personne person) {
        RentalPrincipal principal = new RentalPrincipal(person);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
    private static MockHttpServletRequest request(String method, String path, MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setSession(session);
        return request;
    }
}
