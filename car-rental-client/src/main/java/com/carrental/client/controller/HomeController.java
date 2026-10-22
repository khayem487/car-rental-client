package com.carrental.client.controller;

import com.carrental.client.model.Personne;
import com.carrental.client.service.LocationService;
import com.carrental.client.service.VoitureService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.Collections;

/** Public discovery pages. Personal bookings are loaded only from the current session. */
@Controller
public class HomeController {
    private final VoitureService voitures;
    private final LocationService locations;

    public HomeController(VoitureService voitures, LocationService locations) {
        this.voitures = voitures;
        this.locations = locations;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("voituresDisponibles", voitures.getVoituresDisponibles());
        Personne user = (Personne) session.getAttribute("user");
        model.addAttribute("locationsActives", user == null ? Collections.emptyList()
                : locations.getActiveLocationsByPersonne(user.getId()));
        return "home";
    }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/faq")
    public String faq() { return "faq"; }

    @GetMapping("/contact")
    public String contact() { return "redirect:/about#projet"; }
}
