package com.carrental.client;

import com.carrental.client.model.Personne;
import com.carrental.client.security.StrictPasswordEncoder;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:rental-security;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", "GOOGLE_CLIENT_ID=", "GOOGLE_CLIENT_SECRET="})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class AuthSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;

    @Test void allPublicMutationPathsRejectMissingOrInvalidCsrf() throws Exception {
        Integer peopleBefore = jdbc.queryForObject("SELECT COUNT(*) FROM personne", Integer.class);
        String availabilityBefore = jdbc.queryForObject("SELECT disp FROM voiture WHERE mat='AA-123-BB'", String.class);
        for (String path : new String[]{"/login", "/signup", "/demo", "/profile/update", "/locations/new/AA-123-BB", "/locations/1/terminer", "/logout"}) {
            mvc.perform(post(path)).andExpect(status().isForbidden());
            mvc.perform(post(path).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM personne", Integer.class)).isEqualTo(peopleBefore);
        assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat='AA-123-BB'", String.class)).isEqualTo(availabilityBefore);
    }

    @Test void renderedTokensLoginRotationSessionPasswordsAndPostOnlyLogoutWorkTogether() throws Exception {
        MvcResult page = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        String oldId = session.getId(), oldToken = token(page);
        mvc.perform(post("/login").session(session).param("_csrf", oldToken)
            .param("login", "sophie.martin").param("passwd", "password123")).andExpect(redirectedUrl("/"));
        assertThat(session.getId()).isNotEqualTo(oldId);
        assertThat(((Personne)session.getAttribute("user")).getPasswd()).isNull();
        MvcResult profile = mvc.perform(get("/profile").session(session)).andExpect(status().isOk()).andReturn();
        String freshToken = token(profile);
        assertThat(freshToken).isNotEqualTo(oldToken);
        mvc.perform(post("/profile/update").session(session).param("_csrf", oldToken).param("nom", "Test").param("prenom", "Sophie")).andExpect(status().isForbidden());
        mvc.perform(get("/logout").session(session)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/profile").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).param("_csrf", freshToken)).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/profile")).andExpect(redirectedUrl("/login"));
    }

    @Test void newAccountsUseBcryptAndChangingPasswordRequiresCurrentProofThenEndsSession() throws Exception {
        String login = "security." + UUID.randomUUID().toString().substring(0,8);
        mvc.perform(post("/signup").with(csrf()).param("login",login).param("nom","Test").param("prenom","Compte")
            .param("passwd","TestingPass42!").param("confirmPasswd","TestingPass42!")).andExpect(redirectedUrl("/login"));
        String encoded = jdbc.queryForObject("SELECT passwd FROM personne WHERE login=?", String.class,login);
        assertThat(encoded).startsWith("$2").isNotEqualTo("TestingPass42!");
        assertThat(passwords.matches("TestingPass42!",encoded)).isTrue();
        MockHttpSession session = localLogin(login,"TestingPass42!");
        mvc.perform(post("/profile/update").session(session).with(csrf()).param("nom","Test").param("prenom","Compte")
            .param("passwd","NewTestingPass42!").param("confirmNewPasswd","NewTestingPass42!").param("currentPasswd","wrongpass"))
            .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("error"));
        assertThat(jdbc.queryForObject("SELECT passwd FROM personne WHERE login=?",String.class,login)).isEqualTo(encoded);
        mvc.perform(post("/profile/update").session(session).with(csrf()).param("nom","Test").param("prenom","Compte")
            .param("passwd","NewTestingPass42!").param("confirmNewPasswd","NewTestingPass42!").param("currentPasswd","TestingPass42!"))
            .andExpect(redirectedUrl("/login?passwordChanged"));
        assertThat(session.isInvalid()).isTrue();
        String changed = jdbc.queryForObject("SELECT passwd FROM personne WHERE login=?",String.class,login);
        assertThat(passwords.matches("NewTestingPass42!",changed)).isTrue();
        assertThat(passwords.matches("TestingPass42!",changed)).isFalse();
        mvc.perform(post("/login").with(csrf()).param("login",login).param("passwd","TestingPass42!"))
            .andExpect(redirectedUrl("/login?error"));
        localLogin(login,"NewTestingPass42!");
    }

    @Test void bcryptRejectsLongUnicodeNulPlaintextAndNonClientIdentity() throws Exception {
        String boundary = "é".repeat(36);
        String encoded = passwords.encode(boundary);
        assertThat(passwords.matches(boundary,encoded)).isTrue();
        assertThat(passwords.matches(boundary + "x",encoded)).isFalse();
        assertThat(passwords.matches("a".repeat(73),encoded)).isFalse();
        assertThatThrownBy(() -> passwords.encode(boundary + "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> passwords.encode("abcd\0efgh")).isInstanceOf(IllegalArgumentException.class);
        assertThat(passwords.matches("password123","password123")).isFalse();
        mvc.perform(post("/login").with(csrf()).param("login","admin").param("passwd","admin123"))
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test void aLegacySessionDtoCannotAuthenticateAndGoogleIsAbsentUntilConfigured() throws Exception {
        MockHttpSession forged = new MockHttpSession();
        Personne forgedUser = new Personne(); forgedUser.setId(2L); forgedUser.setRole("CLIENT");
        forged.setAttribute("user",forgedUser);
        mvc.perform(get("/profile").session(forged)).andExpect(redirectedUrl("/login"));
        assertThat(forged.getAttribute("user")).isNull();
        mvc.perform(get("/login")).andExpect(content().string(not(containsString("/oauth2/authorization/google"))));
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().isNotFound());
    }

    private MockHttpSession localLogin(String login,String password) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("login",login).param("passwd",password))
            .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }
    private static String token(MvcResult result) throws Exception {
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).as("server-rendered CSRF token").isTrue();
        return matcher.group(1);
    }
}
