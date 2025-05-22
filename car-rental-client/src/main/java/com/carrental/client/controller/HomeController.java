package com.carrental.client.controller;

import com.carrental.client.model.Location;
import com.carrental.client.model.Personne;
import com.carrental.client.model.Voiture;
import com.carrental.client.service.LocationService;
import com.carrental.client.service.VoitureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur gérant les pages principales du site web.
 * 
 * Ce contrôleur gère:
 * - La page d'accueil
 * - Les pages informatives (à propos, contact, FAQ)
 * 
 * Il s'assure que toutes ces pages ne sont accessibles qu'aux utilisateurs
 * authentifiés et prépare les données nécessaires pour ces vues.
 */
@Controller
public class HomeController {

    /**
     * Service de gestion des voitures
     */
    private final VoitureService voitureService;
    
    /**
     * Service de gestion des locations
     */
    private final LocationService locationService;

    /**
     * Constructeur avec injection des dépendances requises
     * 
     * @param voitureService Service pour accéder aux données des voitures
     * @param locationService Service pour accéder aux données des locations
     */
    @Autowired
    public HomeController(VoitureService voitureService, LocationService locationService) {
        this.voitureService = voitureService;
        this.locationService = locationService;
    }

    /**
     * Gère la requête pour la page d'accueil.
     * Prépare les données suivantes pour la vue:
     * - Informations sur l'utilisateur connecté
     * - Liste des voitures disponibles
     * - Liste des voitures les plus populaires
     * - Liste des locations en cours de l'utilisateur
     * 
     * @param model Le modèle Spring MVC pour transférer des données à la vue
     * @param session La session HTTP contenant les informations de l'utilisateur connecté
     * @return Nom de la vue à afficher, ou redirection vers la page de login si non authentifié
     */
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Vérifier si l'utilisateur est connecté (sécurité)
        if (session.getAttribute("user") == null) {
            System.out.println("Utilisateur non connecté, redirection vers login");
            return "redirect:/login";
        }
        
        try {
            // Récupérer l'utilisateur connecté depuis la session
            Personne personne = (Personne) session.getAttribute("user");
            System.out.println("Accès à la page d'accueil pour: " + personne.getPrenom() + " " + personne.getNom());
            model.addAttribute("personne", personne);
            
            // SECTION 1: Récupération des voitures disponibles
            List<Voiture> voituresDisponibles;
            try {
                voituresDisponibles = voitureService.getVoituresDisponibles();
                System.out.println("Nombre de voitures disponibles: " + voituresDisponibles.size());
            } catch (Exception e) {
                // Gestion des erreurs robuste: en cas d'échec, utiliser une liste vide
                System.err.println("Erreur lors de la récupération des voitures disponibles: " + e.getMessage());
                e.printStackTrace();
                voituresDisponibles = new ArrayList<>();
            }
            model.addAttribute("voituresDisponibles", voituresDisponibles);
            
            // SECTION 2: Récupération des voitures populaires
            List<Voiture> voituresPopulaires;
            try {
                voituresPopulaires = voitureService.getMostRentedVoitures();
                System.out.println("Nombre de voitures populaires: " + voituresPopulaires.size());
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des voitures populaires: " + e.getMessage());
                e.printStackTrace();
                // En cas d'erreur, utiliser une liste vide pour éviter les NullPointerException
                voituresPopulaires = new ArrayList<>();
            }
            model.addAttribute("voituresPopulaires", voituresPopulaires);
            
            // SECTION 3: Récupération des locations actives de l'utilisateur
            List<Location> locationsActives;
            try {
                locationsActives = locationService.getActiveLocationsByPersonne(personne.getId());
                System.out.println("Nombre de locations actives: " + locationsActives.size());
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des locations actives: " + e.getMessage());
                e.printStackTrace();
                // En cas d'erreur, utiliser une liste vide
                locationsActives = new ArrayList<>();
            }
            model.addAttribute("locationsActives", locationsActives);
            
            // Rendu du template home.html avec toutes les données préparées
            System.out.println("Rendu de la page home.html");
            return "home";
        } catch (Exception e) {
            // Gestion des erreurs globale
            System.err.println("Erreur dans la page d'accueil: " + e.getMessage());
            e.printStackTrace();
            
            // En cas d'erreur critique, invalider la session et rediriger vers login
            session.invalidate();
            return "redirect:/login";
        }
    }

    /**
     * Gère la requête pour la page "À propos".
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher, ou redirection vers login si non authentifié
     */
    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        return "about";
    }

    /**
     * Gère la requête pour la page de contact.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher, ou redirection vers login si non authentifié
     */
    @GetMapping("/contact")
    public String contact(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        return "contact";
    }

    /**
     * Gère la requête pour la page FAQ (Foire Aux Questions).
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher, ou redirection vers login si non authentifié
     */
    @GetMapping("/faq")
    public String faq(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        return "faq";
    }
} 