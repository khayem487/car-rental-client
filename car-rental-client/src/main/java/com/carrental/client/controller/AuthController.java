package com.carrental.client.controller;

import com.carrental.client.model.Personne;
import com.carrental.client.service.PersonneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import java.util.UUID;
import java.util.Optional;

/**
 * Contrôleur gérant l'authentification et la gestion des comptes utilisateurs.
 * 
 * Ce contrôleur est responsable de:
 * - L'authentification (login/logout)
 * - L'inscription de nouveaux utilisateurs
 * - La gestion du profil utilisateur
 * 
 * Il utilise les sessions HTTP pour maintenir l'état de connexion
 * et gère les redirections et messages flash pour une expérience
 * utilisateur fluide.
 */
@Controller
public class AuthController {

    /**
     * Service de gestion des personnes pour les opérations d'authentification,
     * d'inscription et de mise à jour du profil.
     */
    private final PersonneService personneService;
    private final Environment environment;

    /**
     * Constructeur avec injection de dépendance
     * 
     * @param personneService Service pour la gestion des utilisateurs
     */
    @Autowired
    public AuthController(PersonneService personneService, Environment environment) {
        this.personneService = personneService;
        this.environment = environment;
    }

    /**
     * Affiche la page de connexion.
     * Si l'utilisateur est déjà connecté, redirige vers la page d'accueil.
     * 
     * @param model Le modèle Spring MVC pour préparer les données pour la vue
     * @param session La session HTTP pour vérifier l'état de connexion
     * @return Nom de la vue à afficher ou redirection
     */
    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session) {
        // Vérifier si l'utilisateur est déjà connecté - dans ce cas, rediriger vers l'accueil
        if (session.getAttribute("user") != null) {
            return "redirect:/";
        }
        
        // Préparer un objet Personne vide pour le formulaire de connexion
        model.addAttribute("personne", new Personne());
        return "login";
    }

    /**
     * Traite la soumission du formulaire de connexion.
     * Authentifie l'utilisateur avec ses identifiants et crée une session.
     * 
     * @param login Identifiant de l'utilisateur
     * @param passwd Mot de passe de l'utilisateur
     * @param session Session HTTP pour stocker les informations de l'utilisateur
     * @param redirectAttributes Pour ajouter des messages flash (affichés après redirection)
     * @return Redirection vers la page d'accueil si authentification réussie, sinon retour à login
     */
    @PostMapping("/login")
    public String processLogin(@RequestParam String login, @RequestParam String passwd, 
                               HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        
        // Tentative d'authentification via le service
        Optional<Personne> personneOpt = personneService.authentifier(login, passwd);
        
        if (personneOpt.isPresent()) {
            // Authentification réussie
            Personne personne = personneOpt.get();
            
            // Stockage de l'utilisateur dans la session pour la durée de sa visite
            request.changeSessionId();
            session.removeAttribute("demoVisitor");
            session.setAttribute("user", personne);
            
            // Redirection vers la page d'accueil
            return "redirect:/";
        } else {
            // Authentification échouée - préparer un message d'erreur
            redirectAttributes.addFlashAttribute("error", "Login ou mot de passe incorrect");
            return "redirect:/login";
        }
    }

    /**
     * Déconnecte l'utilisateur en invalidant sa session.
     * 
     * @param session Session HTTP à invalider
     * @return Redirection vers la page de connexion
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Invalider la session pour déconnecter l'utilisateur
        session.invalidate();
        return "redirect:/";
    }

    /**
     * Affiche la page d'inscription pour les nouveaux utilisateurs.
     * Si l'utilisateur est déjà connecté, redirige vers la page d'accueil.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher ou redirection
     */
    @GetMapping("/signup")
    public String showSignupPage(Model model, HttpSession session) {
        // Vérifier si l'utilisateur est déjà connecté
        if (session.getAttribute("user") != null) {
            return "redirect:/";
        }
        
        // Préparer un objet Personne vide pour le formulaire d'inscription
        model.addAttribute("personne", new Personne());
        return "signup";
    }

    /**
     * Traite la soumission du formulaire d'inscription.
     * Crée un nouveau compte utilisateur avec le rôle CLIENT.
     * 
     * @param personne Données du nouvel utilisateur à enregistrer
     * @param redirectAttributes Pour ajouter des messages flash
     * @return Redirection vers la page de connexion ou d'inscription selon le résultat
     */
    @PostMapping("/signup")
    public String processSignup(@ModelAttribute Personne personne,
                                @RequestParam(defaultValue = "") String confirmPasswd,
                                RedirectAttributes redirectAttributes) {
        try {
            validateName(personne.getNom());
            validateName(personne.getPrenom());
            if (personne.getLogin() == null || !personne.getLogin().matches("[a-zA-Z0-9._-]{3,40}"))
                throw new IllegalArgumentException("Choisissez un identifiant de 3 à 40 lettres, chiffres, points ou tirets.");
            validatePassword(personne.getPasswd());
            if (!personne.getPasswd().equals(confirmPasswd))
                throw new IllegalArgumentException("Les mots de passe ne correspondent pas.");
            if (isDemo()) {
                String ref = "DEMO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                personne.setCin(ref);
                personne.setNpermis(ref);
                personne.setNumero("DEMO");
            } else {
                validateReference(personne.getCin());
                validateReference(personne.getNpermis());
                validateReference(personne.getNumero());
            }
            // Tentative d'enregistrement du nouvel utilisateur
            personneService.enregistrer(personne);
            
            // En cas de succès, rediriger vers la page de connexion avec message de confirmation
            redirectAttributes.addFlashAttribute("success", "Inscription réussie! Vous pouvez maintenant vous connecter.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            // En cas d'erreur (login ou cin déjà utilisé), retourner à la page d'inscription
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

    /**
     * Affiche la page de profil de l'utilisateur connecté.
     * 
     * @param model Le modèle Spring MVC
     * @param session La session HTTP
     * @return Nom de la vue à afficher ou redirection vers login si non connecté
     */
    @GetMapping("/profile")
    public String showProfile(Model model, HttpSession session) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Passer les données de l'utilisateur à la vue
        Personne personne = (Personne) session.getAttribute("user");
        model.addAttribute("personne", personne);
        
        return "profile";
    }

    /**
     * Traite la mise à jour du profil utilisateur.
     * 
     * @param personne Nouvelles données de l'utilisateur
     * @param session Session HTTP contenant l'utilisateur actuel
     * @param redirectAttributes Pour ajouter des messages flash
     * @return Redirection vers la page de profil ou login selon le résultat
     */
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute Personne personne, HttpSession session, RedirectAttributes redirectAttributes) {
        // Vérification de sécurité: l'utilisateur doit être connecté
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        try {
            // Récupérer l'ID de l'utilisateur connecté et l'affecter à l'objet
            // Cela garantit que l'utilisateur ne peut modifier que son propre profil
            Personne currentUser = (Personne) session.getAttribute("user");
            if (Boolean.TRUE.equals(session.getAttribute("demoVisitor")))
                throw new IllegalArgumentException("Le profil de visite est fictif et ne peut pas être modifié.");
            validateName(personne.getNom());
            validateName(personne.getPrenom());
            personne.setId(currentUser.getId());
            personne.setLogin(currentUser.getLogin());
            personne.setCin(currentUser.getCin());
            if (isDemo()) {
                personne.setNumero(currentUser.getNumero());
                personne.setNpermis(currentUser.getNpermis());
            } else {
                validateReference(personne.getNumero());
                validateReference(personne.getNpermis());
            }
            if (personne.getPasswd() == null || personne.getPasswd().isEmpty())
                personne.setPasswd(currentUser.getPasswd());
            else validatePassword(personne.getPasswd());
            
            // Mettre à jour les informations dans la base de données
            personneService.mettreAJour(personne);
            
            // Mettre à jour l'utilisateur dans la session pour refléter les changements
            session.setAttribute("user", personne);
            
            // Rediriger avec un message de succès
            redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            // En cas d'erreur, afficher le message
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }
    private boolean isDemo() { return environment.acceptsProfiles(Profiles.of("demo")); }
    private void validateName(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 50)
            throw new IllegalArgumentException("Renseignez un prénom et un nom de 50 caractères maximum.");
    }
    private void validatePassword(String value) {
        if (value == null || value.length() < 8 || value.length() > 100)
            throw new IllegalArgumentException("Le mot de passe doit contenir entre 8 et 100 caractères.");
    }
    private void validateReference(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 20)
            throw new IllegalArgumentException("Complétez les références et coordonnées (20 caractères maximum).");
    }
}
