package com.carrental.client.controller;

import com.carrental.client.model.Personne;
import com.carrental.client.service.PersonneService;
import com.carrental.client.security.SessionUsers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/** Creates one ordinary fictional CLIENT account per visitor, only in demo mode. */
@Controller
@Profile("demo")
public class DemoAccessController {
    private final PersonneService personnes;
    private final SessionUsers sessions;

    public DemoAccessController(PersonneService personnes, SessionUsers sessions) {
        this.personnes = personnes;
        this.sessions = sessions;
    }

    @PostMapping("/demo")
    public String enter(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        if (session.getAttribute("user") != null) return "redirect:/voitures";

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Personne visitor = new Personne();
        visitor.setPrenom("Visiteur");
        visitor.setNom("Démo");
        visitor.setLogin("demo." + suffix);
        visitor.setPasswd(UUID.randomUUID().toString());
        visitor.setCin("DEMO-" + suffix);
        visitor.setNpermis("DEMO-" + suffix);
        visitor.setNumero("DEMO");
        visitor.setId(personnes.enregistrer(visitor));

        sessions.loginGuest(visitor, request, response);
        return "redirect:/voitures";
    }
}
