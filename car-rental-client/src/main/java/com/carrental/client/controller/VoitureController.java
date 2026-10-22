package com.carrental.client.controller;

import com.carrental.client.service.VoitureService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;
import java.util.stream.Collectors;

/** Public read-only catalogue; booking/account routes remain session protected. */
@Controller
@RequestMapping("/voitures")
public class VoitureController {
    private final VoitureService voitures;

    public VoitureController(VoitureService voitures) {
        this.voitures = voitures;
    }

    @GetMapping
    public String all(Model model) {
        model.addAttribute("voitures", voitures.getAllAvailableVoitures());
        return "voitures/list";
    }

    @GetMapping("/disponibles")
    public String available(Model model) {
        model.addAttribute("voitures", voitures.getVoituresDisponibles());
        model.addAttribute("filtreActif", "disponibles");
        return "voitures/list";
    }

    @GetMapping("/louees")
    public String rented(Model model) {
        model.addAttribute("voitures", voitures.getVoituresLouees());
        model.addAttribute("filtreActif", "louees");
        return "voitures/list";
    }

    @GetMapping("/populaires")
    public String popular(Model model) {
        model.addAttribute("voitures", voitures.getMostRentedVoitures());
        model.addAttribute("filtreActif", "populaires");
        return "voitures/list";
    }

    @GetMapping("/filter")
    public String filter(@RequestParam(defaultValue = "") String marque,
                         @RequestParam(defaultValue = "0") int nbplace,
                         @RequestParam(defaultValue = "0") double prixMax,
                         @RequestParam(defaultValue = "false") boolean disponible,
                         Model model) {
        String query = marque.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("voitures", voitures.getAllAvailableVoitures().stream()
                .filter(v -> (v.getMarque() + " " + v.getModel()).toLowerCase(Locale.ROOT).contains(query))
                .filter(v -> nbplace <= 0 || v.getNbplace() >= nbplace)
                .filter(v -> prixMax <= 0 || v.getPrix() <= prixMax)
                .filter(v -> !disponible || "DISPONIBLE".equals(v.getDisp()))
                .collect(Collectors.toList()));
        model.addAttribute("marque", marque);
        model.addAttribute("nbplace", nbplace);
        model.addAttribute("prixMax", prixMax);
        model.addAttribute("disponibleSeulement", disponible);
        model.addAttribute("filtreActif", "recherche");
        return "voitures/list";
    }

    @GetMapping("/{mat}")
    public String details(@PathVariable String mat, Model model) {
        return voitures.findByMat(mat).filter(v -> !"PANNE".equals(v.getDisp())).map(v -> {
            model.addAttribute("voiture", v);
            model.addAttribute("disponible", "DISPONIBLE".equals(v.getDisp()));
            return "voitures/details";
        }).orElse("redirect:/voitures");
    }
}
