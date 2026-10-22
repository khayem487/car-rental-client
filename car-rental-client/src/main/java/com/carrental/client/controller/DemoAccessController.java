package com.carrental.client.controller;

import com.carrental.client.model.Personne;
import com.carrental.client.service.PersonneService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/** Creates one ordinary fictional CLIENT account per visitor, only in demo mode. */
@Controller
@Profile("demo")
public class DemoAccessController {
    private final PersonneService personnes;

    public DemoAccessController(PersonneService personnes) {
        this.personnes = personnes;
    }

    @PostMapping("/demo")
    public String enter(HttpServletRequest request) {
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

        request.changeSessionId();
        session.setAttribute("user", visitor);
        session.setAttribute("demoVisitor", true);
        return "redirect:/voitures";
    }
}
