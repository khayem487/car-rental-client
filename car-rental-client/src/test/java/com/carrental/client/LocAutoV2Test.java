package com.carrental.client;

import com.carrental.client.model.Personne;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class LocAutoV2Test {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void publicDiscoveryPagesRenderAndPrivateRoutesRequireAnAccount() throws Exception {
        for (String path : new String[]{"/", "/voitures", "/voitures/disponibles", "/voitures/louees",
                "/voitures/populaires", "/voitures/AA-123-BB", "/login", "/signup", "/about", "/faq", "/privacy", "/terms"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("name=\"viewport\"")));
        }
        mvc.perform(get("/contact")).andExpect(redirectedUrl("/about#projet"));
        mvc.perform(get("/signup")).andExpect(content().string(not(containsString("name=\"cin\""))))
                .andExpect(content().string(not(containsString("name=\"npermis\""))));
        for (String path : new String[]{"/locations", "/locations/actives", "/locations/1",
                "/locations/new/AA-123-BB", "/profile"}) {
            mvc.perform(get(path)).andExpect(redirectedUrl("/login"));
        }
        mvc.perform(booking("/locations/new/AA-123-BB", null, LocalDate.now(), "2", "NO-AUTH"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/locations/1/terminer").with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/profile/update").with(csrf()).param("nom", "Forgery")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void catalogueFiltersAreCombinedRatherThanPrioritised() throws Exception {
        mvc.perform(get("/voitures/filter").param("marque", "Clio").param("nbplace", "5")
                .param("prixMax", "55").param("disponible", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("voitures", hasSize(1)));
        mvc.perform(get("/voitures/filter").param("marque", "Clio").param("nbplace", "5")
                .param("prixMax", "40")).andExpect(status().isOk())
                .andExpect(model().attribute("voitures", hasSize(0)));
        mvc.perform(get("/voitures/filter").param("marque", "Renault").param("nbplace", "7"))
                .andExpect(model().attribute("voitures", hasSize(0)));
    }

    @Test
    void localLoginRotatesTheSessionIdentifierAndDoesNotRenderItsPassword() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String oldId = before.getId();
        mvc.perform(post("/login").with(csrf()).session(before).param("login", "sophie.martin")
                .param("passwd", "password123")).andExpect(redirectedUrl("/"));
        assertThat(before.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/profile").session(before)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"password123\""))));
    }

    @Test
    void reservationDisplaysTheExactPriceIncludingCents() throws Exception {
        MockHttpSession session = demoSession();
        jdbc.update("UPDATE voiture SET prix=65.25 WHERE mat='CC-456-DD'");
        try {
            mvc.perform(get("/locations/new/CC-456-DD").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("data-price=\"65.25\"")))
                    .andExpect(content().string(containsString("130,50 €")));
        } finally {
            jdbc.update("UPDATE voiture SET prix=65 WHERE mat='CC-456-DD'");
        }
    }

    @Test
    void oneClickVisitorsHaveIsolatedRealAccountsAndCannotAccessEachOthersBookings() throws Exception {
        MockHttpSession first = demoSession();
        MockHttpSession second = demoSession();
        Personne user1 = (Personne) first.getAttribute("user");
        Personne user2 = (Personne) second.getAttribute("user");
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
        assertThat(user1.getRole()).isEqualTo("CLIENT");
        assertThat(user1.getCin()).startsWith("DEMO-");
        assertThat(user1.getNumero()).isEqualTo("DEMO");
        mvc.perform(get("/profile").session(first)).andExpect(status().isOk());
        mvc.perform(post("/profile/update").with(csrf()).session(first).param("role", "ADMIN").param("nom", "Changed"))
                .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("error"));

        String path = mvc.perform(booking("/locations/new/AA-123-BB", first, LocalDate.now(), "2", "ISOLATION"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(path).startsWith("/locations/");
        try {
            mvc.perform(get(path).session(first)).andExpect(status().isOk());
            mvc.perform(get(path).session(second)).andExpect(redirectedUrl("/locations"));
            mvc.perform(post(path + "/terminer").with(csrf()).session(second)).andExpect(redirectedUrl("/locations"));
            assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat='AA-123-BB'", String.class))
                    .isEqualTo("LOUEE");
            mvc.perform(get("/locations").session(second)).andExpect(model().attribute("totalLocations", 0));
            mvc.perform(post("/demo").with(csrf()).session(first)).andExpect(redirectedUrl("/voitures"));
            assertThat(((Personne) first.getAttribute("user")).getId()).isEqualTo(user1.getId());
        } finally {
            mvc.perform(post(path + "/terminer").with(csrf()).session(first)).andExpect(redirectedUrl("/locations"));
        }
    }

    @Test
    void signupForcesFictionalReferencesAndClientRoleAndRejectsPasswordMismatch() throws Exception {
        String login = "test." + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/signup").with(csrf()).param("login", login).param("nom", "Test").param("prenom", "Compte")
                .param("passwd", "testpassword").param("confirmPasswd", "different"))
                .andExpect(redirectedUrl("/signup")).andExpect(flash().attributeExists("error"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM personne WHERE login=?", Integer.class, login))
                .isZero();
        mvc.perform(post("/signup").with(csrf()).param("login", login).param("nom", "Test").param("prenom", "Compte")
                .param("passwd", "testpassword").param("confirmPasswd", "testpassword")
                .param("id", "1").param("role", "ADMIN").param("nbloc", "999")
                .param("cin", "REAL-ID-ATTEMPT").param("npermis", "REAL-LICENCE").param("numero", "123456"))
                .andExpect(redirectedUrl("/login"));
        assertThat(jdbc.queryForObject("SELECT role FROM personne WHERE login=?", String.class, login))
                .isEqualTo("CLIENT");
        assertThat(jdbc.queryForObject("SELECT nbloc FROM personne WHERE login=?", Integer.class, login))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT cin FROM personne WHERE login=?", String.class, login))
                .startsWith("DEMO-");
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("login", login)
                .param("passwd", "testpassword")).andReturn().getRequest().getSession(false);
        Long id = ((Personne) session.getAttribute("user")).getId();
        mvc.perform(post("/profile/update").with(csrf()).session(session).param("nom", "Modifie").param("prenom", "Compte")
                .param("passwd", "").param("id", "1").param("role", "ADMIN").param("login", "admin")
                .param("cin", "FORGED")).andExpect(redirectedUrl("/profile"));
        assertThat(jdbc.queryForObject("SELECT login FROM personne WHERE id=?", String.class, id)).isEqualTo(login);
        assertThat(jdbc.queryForObject("SELECT role FROM personne WHERE id=?", String.class, id)).isEqualTo("CLIENT");
        assertThat(jdbc.queryForObject("SELECT passwd FROM personne WHERE id=?", String.class, id)).startsWith("$2");
    }

    @Test
    void serverRejectsPastDatesInvalidDurationsAndMissingReferencesWithoutChangingAvailability() throws Exception {
        MockHttpSession session = demoSession();
        for (String duration : new String[]{"0", "-1", "31", "2147483647"}) {
            mvc.perform(booking("/locations/new/AA-123-BB", session, LocalDate.now(), duration, "INVALID"))
                    .andExpect(redirectedUrl("/voitures")).andExpect(flash().attributeExists("error"));
        }
        mvc.perform(booking("/locations/new/AA-123-BB", session, LocalDate.now().minusDays(1), "2", "INVALID"))
                .andExpect(redirectedUrl("/voitures")).andExpect(flash().attributeExists("error"));
        mvc.perform(booking("/locations/new/AA-123-BB", session, LocalDate.now(), "2", ""))
                .andExpect(redirectedUrl("/voitures")).andExpect(flash().attributeExists("error"));
        assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat='AA-123-BB'", String.class))
                .isEqualTo("DISPONIBLE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM location WHERE cheque='INVALID'", Integer.class)).isZero();
    }

    @Test
    void simultaneousBookingsClaimTheVehicleExactlyOnceAndOldReturnCannotReleaseANewBooking() throws Exception {
        MockHttpSession first = demoSession(), second = demoSession();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> firstCall = () -> { start.await(); return mvc.perform(booking("/locations/new/CC-456-DD",
                first, LocalDate.now(), "2", "CONCURRENT")).andReturn().getResponse().getRedirectedUrl(); };
        Callable<String> secondCall = () -> { start.await(); return mvc.perform(booking("/locations/new/CC-456-DD",
                second, LocalDate.now(), "2", "CONCURRENT")).andReturn().getResponse().getRedirectedUrl(); };
        Future<String> a = executor.submit(firstCall), b = executor.submit(secondCall);
        start.countDown();
        String pa, pb;
        try { pa = a.get(10, TimeUnit.SECONDS); pb = b.get(10, TimeUnit.SECONDS); }
        finally { executor.shutdownNow(); }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM location WHERE cheque='CONCURRENT'", Integer.class))
                .isEqualTo(1);
        assertThat(java.util.Arrays.asList(pa, pb).stream().filter(p -> p.startsWith("/locations/")).count())
                .isEqualTo(1);
        String winningPath = pa.startsWith("/locations/") ? pa : pb;
        MockHttpSession winner = pa.startsWith("/locations/") ? first : second;
        mvc.perform(post(winningPath + "/terminer").with(csrf()).session(winner)).andExpect(redirectedUrl("/locations"));
        String newPath = mvc.perform(booking("/locations/new/CC-456-DD", winner, LocalDate.now(), "1", "AFTER-RETURN"))
                .andReturn().getResponse().getRedirectedUrl();
        try {
            mvc.perform(post(winningPath + "/terminer").with(csrf()).session(winner))
                    .andExpect(flash().attributeExists("error"));
            assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat='CC-456-DD'", String.class))
                    .isEqualTo("LOUEE");
        } finally { mvc.perform(post(newPath + "/terminer").with(csrf()).session(winner)); }
    }

    private MockHttpSession demoSession() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String oldId = before.getId();
        mvc.perform(post("/demo").with(csrf()).session(before)).andExpect(redirectedUrl("/voitures"));
        assertThat(before.getId()).isNotEqualTo(oldId);
        assertThat(before.getAttribute("demoVisitor")).isEqualTo(true);
        return before;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder booking(
            String path, MockHttpSession session, LocalDate date, String duration, String reference) {
        var request = post(path).with(csrf()).param("dteDeb", date.toString()).param("duree", duration).param("cheque", reference);
        return session == null ? request : request.session(session);
    }
}
