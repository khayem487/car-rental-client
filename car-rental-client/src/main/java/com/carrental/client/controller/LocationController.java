package com.carrental.client.controller;

import com.carrental.client.model.Location;
import com.carrental.client.model.Personne;
import com.carrental.client.model.Voiture;
import com.carrental.client.service.LocationService;
import com.carrental.client.service.VoitureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur gérant les opérations liées aux locations de voitures.
 * 
 * Ce contrôleur est responsable de:
 * - Afficher toutes les locations d'un utilisateur
 * - Créer de nouvelles locations
 * - Terminer des locations existantes
 * - Afficher les détails d'une location spécifique
 * 
 * Toutes les méthodes vérifient l'authentification de l'utilisateur
 * et s'assurent qu'il accède uniquement à ses propres locations.
 */
@Controller
@RequestMapping("/locations")
public class LocationController {

    /**
     * Service de gestion des locations
     */
    private final LocationService locationService;
    
    /**
     * Service de gestion des voitures
     */
    private final VoitureService voitureService;

    /**
     * Constructeur avec injection des dépendances
     * 
     * @param locationService Service pour les opérations liées aux locations
     * @param voitureService Service pour les opérations liées aux voitures
     */
    @Autowired
    public LocationController(LocationService locationService, VoitureService voitureService) {
        this.locationService = locationService;
        this.voitureService = voitureService;
    }

    /**
     * Affiche toutes les locations de l'utilisateur connecté.
     * Inclut également des statistiques sur ses locations.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping
    public String getAllLocations(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer l'utilisateur connecté depuis la session
        Personne personne = (Personne) session.getAttribute("user");
        
        // Récupérer toutes les locations de l'utilisateur (actives et terminées)
        List<Location> locations = locationService.getLocationsByPersonne(personne.getId());
        model.addAttribute("locations", locations);
        
        // Calculer les statistiques pour la vue
        // (nombre total de locations, nombre de locations actives, nombre de locations terminées)
        int totalLocations = locations.size();
        int activeLocations = 0;
        int completedLocations = 0;
        
        for (Location loc : locations) {
            if ("EN_COURS".equals(loc.getEtat())) {
                activeLocations++;
            } else if ("TERMINEE".equals(loc.getEtat())) {
                completedLocations++;
            }
        }
        
        // Passer les statistiques au modèle pour affichage dans la vue
        model.addAttribute("totalLocations", totalLocations);
        model.addAttribute("activeLocations", activeLocations);
        model.addAttribute("completedLocations", completedLocations);
        
        return "locations/list";
    }

    /**
     * Affiche uniquement les locations actives (EN_COURS) de l'utilisateur.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping("/actives")
    public String getActiveLocations(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer l'utilisateur connecté depuis la session
        Personne personne = (Personne) session.getAttribute("user");
        
        // Récupérer uniquement les locations actives de l'utilisateur
        List<Location> locations = locationService.getActiveLocationsByPersonne(personne.getId());
        model.addAttribute("locations", locations);
        model.addAttribute("activeOnly", true);  // Drapeau pour la vue, indiquant qu'on affiche seulement les locations actives
        
        // Calcul des statistiques (simplifié car toutes les locations sont actives)
        int totalLocations = locations.size();
        int activeLocations = totalLocations; // Toutes sont actives dans ce cas
        int completedLocations = 0;
        
        model.addAttribute("totalLocations", totalLocations);
        model.addAttribute("activeLocations", activeLocations);
        model.addAttribute("completedLocations", completedLocations);
        
        return "locations/list";
    }

    /**
     * Affiche les détails d'une location spécifique.
     * Vérifie que l'utilisateur est bien le propriétaire de cette location.
     * 
     * @param id Identifiant de la location
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher ou redirection
     */
    @GetMapping("/{id}")
    public String getLocationDetails(@PathVariable Long id, Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer l'utilisateur connecté depuis la session
        Personne personne = (Personne) session.getAttribute("user");
        
        // Récupérer la location spécifiée par son identifiant
        Optional<Location> locationOpt = locationService.findById(id);
        
        if (locationOpt.isPresent()) {
            Location location = locationOpt.get();
            
            // Vérification de sécurité: s'assurer que la location appartient bien à l'utilisateur courant
            // Empêche l'accès aux locations d'autres utilisateurs
            if (!location.getId_personne().equals(personne.getId())) {
                return "redirect:/locations";
            }
            
            // Préparer les données pour la vue
            model.addAttribute("location", location);
            return "locations/details";
        } else {
            // Si la location n'existe pas, rediriger vers la liste
            return "redirect:/locations";
        }
    }

    /**
     * Affiche le formulaire de création d'une nouvelle location pour une voiture spécifique.
     * Vérifie d'abord que la voiture existe et est disponible.
     * 
     * @param mat Immatriculation de la voiture à louer
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher ou redirection
     */
    @GetMapping("/new/{mat}")
    public String showLocationForm(@PathVariable String mat, Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer les informations de la voiture à louer
        Optional<Voiture> voitureOpt = voitureService.findByMat(mat);
        
        if (voitureOpt.isPresent()) {
            Voiture voiture = voitureOpt.get();
            
            // Vérifier que la voiture est bien disponible pour la location
            // (protection contre les manipulations directes d'URL)
            if (!"DISPONIBLE".equals(voiture.getDisp())) {
                return "redirect:/voitures";
            }
            
            // Préparer les données pour le formulaire
            model.addAttribute("voiture", voiture);
            model.addAttribute("location", new Location());  // Objet vide pour binding du formulaire
            
            return "locations/form";
        } else {
            // Si la voiture n'existe pas, rediriger vers la liste des voitures
            return "redirect:/voitures";
        }
    }

    /**
     * Traite la soumission du formulaire de création d'une location.
     * Valide les données et crée une nouvelle location.
     * 
     * @param mat Immatriculation de la voiture à louer
     * @param dteDeb Date de début de la location
     * @param duree Durée de la location en jours
     * @param cheque Numéro de chèque de caution
     * @param session La session HTTP
     * @param redirectAttributes Pour passer des messages lors de la redirection
     * @return Redirection vers la page appropriée
     */
    @PostMapping("/new/{mat}")
    public String createLocation(
            @PathVariable String mat,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dteDeb,
            @RequestParam int duree,
            @RequestParam String cheque,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer l'utilisateur connecté depuis la session
        Personne personne = (Personne) session.getAttribute("user");
        
        try {
            // Créer un nouvel objet Location avec les données du formulaire
            Location location = new Location();
            location.setId_personne(personne.getId());
            location.setMat(mat);
            location.setDteDeb(dteDeb);
            location.setDuree(duree);
            location.setCheque(cheque);
            location.setEtat("EN_COURS");
            
            // Créer la location dans le système et récupérer son identifiant
            Long id = locationService.createLocation(location);
            
            // Préparer un message de confirmation pour l'utilisateur
            redirectAttributes.addFlashAttribute("success", "Voiture louée avec succès pour " + duree + " jours");
            
            // Rediriger vers les détails de la nouvelle location
            return "redirect:/locations/" + id;
        } catch (IllegalArgumentException e) {
            // En cas d'erreur (ex: voiture non disponible), afficher le message d'erreur
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/voitures";
        }
    }

    /**
     * Termine une location existante.
     * Vérifie que l'utilisateur est bien le propriétaire de cette location.
     * 
     * @param id Identifiant de la location à terminer
     * @param session La session HTTP
     * @param redirectAttributes Pour passer des messages lors de la redirection
     * @return Redirection vers la liste des locations
     */
    @PostMapping("/{id}/terminer")
    public String terminerLocation(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer l'utilisateur connecté depuis la session
        Personne personne = (Personne) session.getAttribute("user");
        
        try {
            // Récupérer la location à terminer
            Optional<Location> locationOpt = locationService.findById(id);
            
            if (locationOpt.isPresent()) {
                Location location = locationOpt.get();
                
                // Vérification de sécurité: s'assurer que la location appartient bien à l'utilisateur courant
                // Empêche la terminaison des locations d'autres utilisateurs
                if (!location.getId_personne().equals(personne.getId())) {
                    return "redirect:/locations";
                }
                
                // Terminer la location (ce qui rend également la voiture disponible)
                locationService.terminerLocation(id);
                
                // Préparer un message de confirmation
                redirectAttributes.addFlashAttribute("success", "Location terminée avec succès");
                
                return "redirect:/locations";
            } else {
                // Si la location n'existe pas, rediriger vers la liste
                return "redirect:/locations";
            }
        } catch (IllegalArgumentException e) {
            // En cas d'erreur, afficher le message
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/locations";
        }
    }
} 