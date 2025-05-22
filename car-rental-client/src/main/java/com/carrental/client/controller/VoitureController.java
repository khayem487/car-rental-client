package com.carrental.client.controller;

import com.carrental.client.model.Personne;
import com.carrental.client.model.Voiture;
import com.carrental.client.service.VoitureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur gérant l'affichage et le filtrage des voitures.
 * 
 * Ce contrôleur est responsable de:
 * - Afficher la liste des voitures avec différents filtres
 * - Présenter les détails d'une voiture spécifique
 * - Fournir des fonctionnalités de recherche et filtrage
 * 
 * Toutes les méthodes vérifient que l'utilisateur est bien connecté
 * avant de donner accès aux fonctionnalités.
 */
@Controller
@RequestMapping("/voitures")
public class VoitureController {

    /**
     * Service de gestion des voitures
     */
    private final VoitureService voitureService;

    /**
     * Constructeur avec injection de dépendance
     * 
     * @param voitureService Service pour accéder aux données des voitures
     */
    @Autowired
    public VoitureController(VoitureService voitureService) {
        this.voitureService = voitureService;
    }

    /**
     * Affiche la liste de toutes les voitures disponibles et louées
     * (exclut automatiquement les voitures en panne).
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping
    public String getAllVoitures(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer toutes les voitures disponibles (pas en panne)
        List<Voiture> voitures = voitureService.getAllAvailableVoitures();
        model.addAttribute("voitures", voitures);
        
        return "voitures/list";
    }

    /**
     * Affiche uniquement les voitures actuellement disponibles à la location.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping("/disponibles")
    public String getVoituresDisponibles(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer uniquement les voitures disponibles (état "DISPONIBLE")
        List<Voiture> voitures = voitureService.getVoituresDisponibles();
        model.addAttribute("voitures", voitures);
        model.addAttribute("filtreActif", "disponibles");
        
        return "voitures/list";
    }

    /**
     * Affiche uniquement les voitures actuellement en location.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping("/louees")
    public String getVoituresLouees(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer uniquement les voitures louées (état "LOUEE")
        List<Voiture> voitures = voitureService.getVoituresLouees();
        model.addAttribute("voitures", voitures);
        model.addAttribute("filtreActif", "louees");
        
        return "voitures/list";
    }

    /**
     * Affiche les voitures les plus populaires/demandées.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping("/populaires")
    public String getVoituresPopulaires(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer les voitures les plus demandées (basé sur le nombre de locations)
        List<Voiture> voitures = voitureService.getMostRentedVoitures();
        model.addAttribute("voitures", voitures);
        model.addAttribute("filtreActif", "populaires");
        
        return "voitures/list";
    }

    /**
     * Permet de filtrer les voitures selon différents critères:
     * - Par marque (recherche partielle)
     * - Par nombre de places
     * - Par prix maximum
     * 
     * @param marque Marque à rechercher (optionnel)
     * @param nbplace Nombre de places (optionnel)
     * @param prixMax Prix maximum (optionnel)
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher
     */
    @GetMapping("/filter")
    public String filterVoitures(
            @RequestParam(required = false) String marque, 
            @RequestParam(required = false, defaultValue = "0") int nbplace,
            @RequestParam(required = false, defaultValue = "0") double prixMax,
            Model model, HttpSession session) {
        
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        List<Voiture> voitures;
        
        // Application sélective des filtres en fonction des paramètres fournis
        // Priorité: marque > nbplace > prixMax
        if (marque != null && !marque.isEmpty()) {
            // Filtrage par marque (recherche partielle)
            voitures = voitureService.filterByMarque(marque);
            model.addAttribute("filtreActif", "marque");
            model.addAttribute("filtreValeur", marque);
        } else if (nbplace > 0) {
            // Filtrage par nombre de places
            voitures = voitureService.filterByNbPlace(nbplace);
            model.addAttribute("filtreActif", "nbplace");
            model.addAttribute("filtreValeur", nbplace);
        } else if (prixMax > 0) {
            // Filtrage par prix maximum
            voitures = voitureService.filterByPrixMax(prixMax);
            model.addAttribute("filtreActif", "prixMax");
            model.addAttribute("filtreValeur", prixMax);
        } else {
            // Si aucun filtre n'est spécifié, afficher toutes les voitures
            voitures = voitureService.getAllAvailableVoitures();
        }
        
        model.addAttribute("voitures", voitures);
        return "voitures/list";
    }

    /**
     * Affiche les détails d'une voiture spécifique identifiée par son immatriculation.
     * Vérifie également que la voiture n'est pas en panne avant d'afficher ses détails.
     * 
     * @param mat Immatriculation de la voiture à afficher
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher ou redirection
     */
    @GetMapping("/{mat}")
    public String getVoitureDetails(@PathVariable String mat, Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Récupérer la voiture par son immatriculation (identifiant unique)
        Optional<Voiture> voitureOpt = voitureService.findByMat(mat);
        
        if (voitureOpt.isPresent()) {
            Voiture voiture = voitureOpt.get();
            
            // Vérification de sécurité supplémentaire: les voitures en PANNE ne doivent pas être visibles
            if ("PANNE".equals(voiture.getDisp())) {
                return "redirect:/voitures";
            }
            
            // Préparer les données pour la vue
            model.addAttribute("voiture", voiture);
            
            // Indiquer si la voiture est disponible pour la location
            // (utilisé dans la vue pour afficher ou non le bouton de location)
            model.addAttribute("disponible", "DISPONIBLE".equals(voiture.getDisp()));
            
            return "voitures/details";
        } else {
            // Si la voiture n'existe pas, rediriger vers la liste complète
            return "redirect:/voitures";
        }
    }
} 