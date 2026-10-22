package com.carrental.client;

import com.carrental.client.security.OidcAccounts;
import com.carrental.client.security.RentalOidcUser;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Mock verified OIDC claims only; no request is made to Google or a real account. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:rental-oidc;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", "GOOGLE_CLIENT_ID=test-google-client", "GOOGLE_CLIENT_SECRET=fictional-test-secret"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class GoogleOidcTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OidcAccounts accounts;

    @Test void googleLoginStartsOnlyConfiguredOidcWithStateAndNonceAndNoGmailScope() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(containsString("/oauth2/authorization/google")));
        String location = mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
            .contains("state=", "nonce=", "openid", "profile", "email", "redirect_uri=")
            .doesNotContain("gmail", "fictional-test-secret");
    }

    @Test void subjectIsStableEmailIsVerifiedAndNeverUsedForAutomaticLocalAccountLinking() {
        String subject = "sub-"+UUID.randomUUID();
        Long first = accounts.resolve(identity(subject,"same@example.test",true,"https://accounts.google.com"));
        Long renamed = accounts.resolve(identity(subject,"changed@example.test",true,"https://accounts.google.com"));
        Long differentSubject = accounts.resolve(identity("other-"+UUID.randomUUID(),"same@example.test",true,"https://accounts.google.com"));
        assertThat(first).isEqualTo(renamed).isNotEqualTo(differentSubject);
        assertThat(jdbc.queryForObject("SELECT role FROM personne WHERE id=?",String.class,first)).isEqualTo("CLIENT");
        assertThat(jdbc.queryForObject("SELECT cin FROM personne WHERE id=?",String.class,first)).startsWith("OIDC-");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oidc_identity WHERE subject=?",Integer.class,subject)).isEqualTo(1);
        assertThatThrownBy(() -> accounts.resolve(identity(subject,"same@example.test",false,"https://accounts.google.com")))
            .isInstanceOf(OAuth2AuthenticationException.class);
        assertThatThrownBy(() -> accounts.resolve(identity(subject,"same@example.test",true,"https://evil.example")))
            .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test void googlePrincipalGetsTheSamePrivateBookingProtectionAndCannotSetLocalPassword() throws Exception {
        OidcUser identity = identity("flow-"+UUID.randomUUID(),"flow@example.test",true,"https://accounts.google.com");
        Long id = accounts.resolve(identity);
        RentalOidcUser principal = new RentalOidcUser(identity,id);
        mvc.perform(get("/profile").with(oidcLogin().oidcUser(principal))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Connexion Google")))
            .andExpect(content().string(not(containsString("name=\"passwd\""))));
        mvc.perform(post("/profile/update").with(oidcLogin().oidcUser(principal)).param("nom","Test").param("prenom","Compte"))
            .andExpect(status().isForbidden());
        String before = jdbc.queryForObject("SELECT passwd FROM personne WHERE id=?",String.class,id);
        mvc.perform(post("/profile/update").with(oidcLogin().oidcUser(principal)).with(csrf())
            .param("nom","Test").param("prenom","Compte").param("passwd","FakeNewPass42!").param("confirmNewPasswd","FakeNewPass42!"))
            .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("error"));
        assertThat(jdbc.queryForObject("SELECT passwd FROM personne WHERE id=?",String.class,id)).isEqualTo(before);
    }

    private static OidcUser identity(String subject,String email,boolean verified,String issuer) {
        Instant now = Instant.now();
        OidcIdToken token = new OidcIdToken("mock-id-token",now,now.plusSeconds(300),
            Map.of("iss",issuer,"sub",subject,"email",email,"email_verified",verified,"given_name","Compte","family_name","Test"));
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),token);
    }
}
