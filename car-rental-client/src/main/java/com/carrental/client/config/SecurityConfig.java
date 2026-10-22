package com.carrental.client.config;

import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.security.*;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new StrictPasswordEncoder(); }
    @Bean CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean UserDetailsService users(PersonneDAO dao) {
        return login -> new RentalPrincipal(dao.findByLogin(login).filter(person -> "CLIENT".equals(person.getRole()))
            .orElseThrow(() -> new UsernameNotFoundException("Identifiants incorrects")));
    }
    @Bean AuthenticationProvider localAuthentication(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }
    @Bean AuthenticationSuccessHandler browserSuccess(PersonneDAO dao) {
        return (request, response, auth) -> {
            Long id = SessionUsers.id(auth);
            var person = dao.findById(id).orElseThrow();
            request.getSession().removeAttribute("demoVisitor");
            request.getSession().setAttribute("user", SessionUsers.withoutPassword(person));
            response.sendRedirect(request.getContextPath() + "/");
        };
    }
    @Bean SecurityFilterChain browserSecurity(HttpSecurity http, PersonneDAO dao,
        AuthenticationProvider localAuthentication, AuthenticationSuccessHandler browserSuccess,
        CsrfTokenRepository csrf, ObjectProvider<ClientRegistrationRepository> google, OidcAccounts accounts) throws Exception {
        http.authenticationProvider(localAuthentication)
            .authorizeHttpRequests(auth -> auth.requestMatchers("/profile/**", "/locations/**").hasRole("CLIENT").anyRequest().permitAll())
            .csrf(config -> config.csrfTokenRepository(csrf))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").usernameParameter("login").passwordParameter("passwd")
                .successHandler(browserSuccess).failureUrl("/login?error").permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .addFilterAfter(new SessionUserFilter(dao), AnonymousAuthenticationFilter.class);
        if (google.getIfAvailable() != null) {
            OidcUserService delegate = new OidcUserService();
            http.oauth2Login(oidc -> oidc.loginPage("/login").successHandler(browserSuccess).failureUrl("/login?error")
                .userInfoEndpoint(info -> info.oidcUserService(request -> {
                    var identity = delegate.loadUser(request);
                    return new RentalOidcUser(identity, accounts.resolve(identity));
                })));
        }
        return http.build();
    }
}
