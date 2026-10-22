package com.carrental.client.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.time.LocalDate;

/** Shared display metadata makes no assumptions about durable storage or external identity. */
@ControllerAdvice
public class UiModelAdvice {
    private final Environment environment;

    public UiModelAdvice(Environment environment) {
        this.environment = environment;
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return environment.acceptsProfiles(Profiles.of("demo"));
    }

    @ModelAttribute("today")
    public String today() { return LocalDate.now().toString(); }
}
