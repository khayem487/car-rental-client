package com.carrental.client.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class GoogleLoginConfiguration {
    public static class Configured implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String id = context.getEnvironment().getProperty("GOOGLE_CLIENT_ID", "");
            String secret = context.getEnvironment().getProperty("GOOGLE_CLIENT_SECRET", "");
            return !id.isBlank() && !secret.isBlank();
        }
    }
    @Bean @Conditional(Configured.class)
    ClientRegistrationRepository googleClients(Environment environment) {
        var registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId(environment.getRequiredProperty("GOOGLE_CLIENT_ID"))
            .clientSecret(environment.getRequiredProperty("GOOGLE_CLIENT_SECRET"))
            .scope("openid", "profile", "email")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
