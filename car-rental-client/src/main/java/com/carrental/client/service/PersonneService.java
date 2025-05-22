package com.carrental.client.service;

import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.model.Personne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service de gestion des utilisateurs (personnes).
 * 
 * Cette classe implémente la logique métier liée aux utilisateurs:
 * - Authentification et contrôle d'accès
 * - Inscription et gestion des comptes utilisateurs
 * - Mise à jour des profils
 * 
 * Elle gère spécifiquement les aspects concernant les clients,
 * en garantissant que seuls les utilisateurs ayant le rôle CLIENT
 * peuvent accéder à certaines fonctionnalités de l'application.
 * 
 * Ce service sert de couche intermédiaire entre les contrôleurs
 * et la couche d'accès aux données, apportant la logique métier
 * nécessaire aux opérations sur les utilisateurs.
 */
@Service
public class PersonneService {

    /**
     * Accès aux données des personnes
     */
    private final PersonneDAO personneDAO;

    /**
     * Constructeur avec injection de dépendance pour le DAO des personnes.
     * 
     * @param personneDAO DAO pour l'accès aux données des personnes
     */
    @Autowired
    public PersonneService(PersonneDAO personneDAO) {
        this.personneDAO = personneDAO;
    }

    /**
     * Authentifie un utilisateur en vérifiant ses identifiants.
     * Cette méthode délègue la vérification au DAO qui effectue la requête
     * à la base de données pour confirmer la correspondance login/mot de passe.
     * 
     * @param login Identifiant de connexion de l'utilisateur
     * @param passwd Mot de passe de l'utilisateur (non chiffré dans cette version)
     * @return Optional contenant l'utilisateur si authentification réussie, vide sinon
     */
    public Optional<Personne> authentifier(String login, String passwd) {
        return personneDAO.authentifier(login, passwd);
    }

    /**
     * Vérifie si un utilisateur a le rôle CLIENT.
     * Cette méthode est utilisée pour s'assurer qu'un utilisateur
     * a les droits appropriés pour accéder aux fonctionnalités client.
     * 
     * @param personne Utilisateur à vérifier
     * @return true si l'utilisateur a le rôle CLIENT, false sinon
     */
    public boolean isClient(Personne personne) {
        return "CLIENT".equals(personne.getRole());
    }

    /**
     * Enregistre un nouvel utilisateur client dans le système.
     * Cette méthode:
     * 1. Vérifie que le login n'est pas déjà utilisé
     * 2. Vérifie que le CIN n'est pas déjà enregistré
     * 3. Définit le rôle de l'utilisateur comme CLIENT
     * 4. Initialise son compteur de locations à 0
     * 5. Persiste l'utilisateur dans la base de données
     * 
     * @param personne Informations du nouvel utilisateur à enregistrer
     * @return ID généré pour le nouvel utilisateur
     * @throws IllegalArgumentException si le login ou le CIN sont déjà utilisés
     */
    public Long enregistrer(Personne personne) {
        // VALIDATION 1: Vérifier l'unicité du login
        if (personneDAO.loginExists(personne.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        
        // VALIDATION 2: Vérifier l'unicité du CIN
        if (personneDAO.cinExists(personne.getCin())) {
            throw new IllegalArgumentException("Ce CIN est déjà utilisé");
        }
        
        // INITIALISATION: Configurer les valeurs par défaut pour un nouveau client
        personne.setRole("CLIENT");  // Rôle par défaut pour les utilisateurs de cette interface
        personne.setNbloc(0);        // Nombre initial de locations = 0
        
        // PERSISTANCE: Enregistrer le nouvel utilisateur
        return personneDAO.save(personne);
    }

    /**
     * Met à jour les informations d'un utilisateur existant.
     * Cette méthode:
     * 1. Vérifie que l'utilisateur existe
     * 2. Préserve son rôle original (empêche le changement de rôle)
     * 3. Préserve son compteur de locations
     * 4. Persiste les modifications dans la base de données
     * 
     * @param personne Objet Personne avec les nouvelles informations
     * @throws IllegalArgumentException si l'utilisateur n'existe pas
     */
    public void mettreAJour(Personne personne) {
        // VALIDATION: Vérifier que l'utilisateur existe
        Optional<Personne> existingPersonne = personneDAO.findById(personne.getId());
        
        if (existingPersonne.isPresent()) {
            // PRÉSERVATION: Maintenir les valeurs critiques qui ne doivent pas être modifiées
            personne.setRole(existingPersonne.get().getRole());     // Empêcher le changement de rôle
            personne.setNbloc(existingPersonne.get().getNbloc());   // Préserver le compteur de locations
            
            // PERSISTANCE: Mettre à jour l'utilisateur
            personneDAO.update(personne);
        } else {
            throw new IllegalArgumentException("Personne non trouvée");
        }
    }

    /**
     * Recherche un utilisateur par son identifiant unique.
     * Simple délégation au DAO correspondant.
     * 
     * @param id Identifiant de l'utilisateur à rechercher
     * @return Optional contenant l'utilisateur si trouvé, vide sinon
     */
    public Optional<Personne> findById(Long id) {
        return personneDAO.findById(id);
    }
} 