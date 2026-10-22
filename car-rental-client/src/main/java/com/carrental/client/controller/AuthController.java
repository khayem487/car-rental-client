package com.carrental.client.controller;

import com.carrental.client.model.Personne;
import com.carrental.client.security.RentalOidcUser;
import com.carrental.client.security.SessionUsers;
import com.carrental.client.security.StrictPasswordEncoder;
import com.carrental.client.service.PersonneService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Spring Security handles login/OIDC/logout; this controller manages safe account forms. */
@Controller
public class AuthController {
    private final PersonneService personnes;
    private final Environment environment;
    public AuthController(PersonneService personnes, Environment environment) { this.personnes = personnes; this.environment = environment; }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        if (session.getAttribute("user") != null) return "redirect:/";
        model.addAttribute("personne", new Personne());
        return "login";
    }
    /** GET never ends an authenticated session; actual logout requires POST and CSRF. */
    @GetMapping("/logout")
    public String logoutPage() { return "redirect:/profile"; }

    @GetMapping("/signup")
    public String signup(Model model, HttpSession session) {
        if (session.getAttribute("user") != null) return "redirect:/";
        model.addAttribute("personne", new Personne());
        return "signup";
    }
    @PostMapping("/signup")
    public String register(@ModelAttribute Personne person, @RequestParam(defaultValue="") String confirmPasswd, RedirectAttributes flash) {
        try {
            validateName(person.getNom()); validateName(person.getPrenom());
            if (person.getLogin() == null || !person.getLogin().matches("[a-zA-Z0-9._-]{3,40}")) throw new IllegalArgumentException("Choisissez un identifiant de 3 à 40 lettres, chiffres, points ou tirets.");
            validatePassword(person.getPasswd());
            if (!person.getPasswd().equals(confirmPasswd)) throw new IllegalArgumentException("Les mots de passe ne correspondent pas.");
            if (isDemo()) {
                String ref = "DEMO-" + UUID.randomUUID().toString().replace("-", "").substring(0,12);
                person.setCin(ref); person.setNpermis(ref); person.setNumero("DEMO");
            } else { validateReference(person.getCin()); validateReference(person.getNpermis()); validateReference(person.getNumero()); }
            personnes.enregistrer(person);
            flash.addFlashAttribute("success", "Inscription réussie. Vous pouvez maintenant vous connecter.");
            return "redirect:/login";
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage()); return "redirect:/signup";
        }
    }
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/login";
        model.addAttribute("personne", session.getAttribute("user"));
        return "profile";
    }
    @PostMapping("/profile/update")
    public String update(@ModelAttribute Personne person, @RequestParam(defaultValue="") String currentPasswd,
        @RequestParam(defaultValue="") String confirmNewPasswd, HttpSession session, HttpServletRequest request,
        Authentication authentication, RedirectAttributes flash) {
        Personne current = (Personne) session.getAttribute("user");
        if (current == null) return "redirect:/login";
        try {
            if (Boolean.TRUE.equals(session.getAttribute("demoVisitor"))) throw new IllegalArgumentException("Le profil de visite est fictif et ne peut pas être modifié.");
            validateName(person.getNom()); validateName(person.getPrenom());
            person.setId(current.getId()); person.setLogin(current.getLogin()); person.setCin(current.getCin());
            if (isDemo()) { person.setNumero(current.getNumero()); person.setNpermis(current.getNpermis()); }
            else { validateReference(person.getNumero()); validateReference(person.getNpermis()); }
            boolean passwordChange = person.getPasswd() != null && !person.getPasswd().isEmpty();
            if (passwordChange) {
                if (authentication.getPrincipal() instanceof RentalOidcUser) throw new IllegalArgumentException("Ce compte utilise Google. Son mot de passe se gère auprès de Google.");
                validatePassword(person.getPasswd());
                if (!person.getPasswd().equals(confirmNewPasswd)) throw new IllegalArgumentException("Les nouveaux mots de passe ne correspondent pas.");
                if (personnes.authentifier(current.getLogin(), currentPasswd).isEmpty()) throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
            }
            personnes.mettreAJour(person);
            if (passwordChange) {
                SecurityContextHolder.clearContext(); session.invalidate();
                return "redirect:/login?passwordChanged";
            }
            session.setAttribute("user", SessionUsers.withoutPassword(personnes.findById(current.getId()).orElseThrow()));
            flash.addFlashAttribute("success", "Profil mis à jour avec succès.");
            return "redirect:/profile";
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage()); return "redirect:/profile";
        }
    }
    private boolean isDemo() { return environment.acceptsProfiles(Profiles.of("demo")); }
    private static void validateName(String value) {
        if (value == null || value.isBlank() || value.length() > 50) throw new IllegalArgumentException("Renseignez un prénom et un nom de 50 caractères maximum.");
    }
    private static void validatePassword(String value) {
        if (!StrictPasswordEncoder.valid(value)) throw new IllegalArgumentException("Utilisez au moins 8 caractères et 72 octets UTF-8 maximum pour le mot de passe.");
    }
    private static void validateReference(String value) {
        if (value == null || value.isBlank() || value.length() > 20) throw new IllegalArgumentException("Complétez les références et coordonnées (20 caractères maximum).");
    }
}