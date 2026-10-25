package com.carrental.client.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.time.LocalDate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.core.Authentication;
import com.carrental.client.security.RentalOidcUser;

/** Shared display metadata makes no assumptions about durable storage or external identity. */
@ControllerAdvice
public class UiModelAdvice {
    private final Environment environment;
    private final ObjectProvider<ClientRegistrationRepository> clients;

    public UiModelAdvice(Environment environment, ObjectProvider<ClientRegistrationRepository> clients) {
        this.environment = environment;
        this.clients = clients;
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return environment.acceptsProfiles(Profiles.of("demo", "persistent"));
    }

    @ModelAttribute("durableStorage")
    public boolean durableStorage() { return environment.acceptsProfiles(Profiles.of("persistent")); }

    @ModelAttribute("today")
    public String today() { return LocalDate.now().toString(); }
    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() { return clients.getIfAvailable() != null; }
    @ModelAttribute("googleAccount")
    public boolean googleAccount(Authentication authentication) { return authentication != null && authentication.getPrincipal() instanceof RentalOidcUser; }
}
