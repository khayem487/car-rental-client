package com.carrental.client.service;

import com.carrental.client.dao.LocationDAO;
import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.dao.VoitureDAO;
import com.carrental.client.model.Location;
import com.carrental.client.model.Personne;
import com.carrental.client.model.Voiture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des locations de voitures.
 * 
 * Cette classe implémente la logique métier liée aux locations :
 * - Création et gestion du cycle de vie des locations
 * - Validation des règles métier (voiture disponible, personne existante)
 * - Calcul des montants de location
 * - Coordination entre les différentes entités (Personne, Voiture, Location)
 * 
 * Elle sert d'intermédiaire entre les contrôleurs et la couche d'accès aux données,
 * fournissant une API cohérente et encapsulant les détails d'implémentation.
 */
@Service
public class LocationService {

    /**
     * Accès aux données des locations
     */
    private final LocationDAO locationDAO;
    
    /**
     * Accès aux données des voitures
     */
    private final VoitureDAO voitureDAO;
    
    /**
     * Accès aux données des personnes
     */
    private final PersonneDAO personneDAO;
    
    /**
     * Service de gestion des voitures (utilisé pour vérifier la disponibilité)
     */
    private final VoitureService voitureService;

    /**
     * Constructeur avec injection des dépendances nécessaires.
     * L'annotation @Autowired permet à Spring d'injecter automatiquement les instances requises.
     * 
     * @param locationDAO Accès aux données des locations
     * @param voitureDAO Accès aux données des voitures
     * @param personneDAO Accès aux données des personnes
     * @param voitureService Service de gestion des voitures
     */
    @Autowired
    public LocationService(LocationDAO locationDAO, VoitureDAO voitureDAO, PersonneDAO personneDAO, VoitureService voitureService) {
        this.locationDAO = locationDAO;
        this.voitureDAO = voitureDAO;
        this.personneDAO = personneDAO;
        this.voitureService = voitureService;
    }

    /**
     * Récupère toutes les locations d'une personne, quel que soit leur état.
     * Cette méthode délègue simplement l'appel au DAO approprié.
     * 
     * @param id_personne ID de la personne
     * @return Liste des locations de la personne (actives et terminées)
     */
    public List<Location> getLocationsByPersonne(Long id_personne) {
        return locationDAO.findByPersonneId(id_personne);
    }

    /**
     * Récupère uniquement les locations en cours (actives) d'une personne.
     * Utile pour afficher seulement les locations qui ne sont pas terminées.
     * 
     * @param id_personne ID de la personne
     * @return Liste des locations en cours de la personne
     */
    public List<Location> getActiveLocationsByPersonne(Long id_personne) {
        return locationDAO.findActiveByPersonneId(id_personne);
    }

    /**
     * Crée une nouvelle location après avoir vérifié les conditions métier.
     * Cette méthode:
     * 1. Vérifie que la voiture est disponible
     * 2. Vérifie que la personne existe
     * 3. Définit l'état initial de la location
     * 4. Sauvegarde la location
     * 5. Met à jour les statistiques de la personne
     * 
     * @param location Objet Location contenant les informations de base
     * @return ID de la nouvelle location créée
     * @throws IllegalArgumentException si la voiture n'est pas disponible ou si la personne n'existe pas
     */
    public Long createLocation(Location location) {
        // VALIDATION 1: Vérifier que la voiture est bien disponible
        if (!voitureService.isVoitureDisponible(location.getMat())) {
            throw new IllegalArgumentException("Cette voiture n'est pas disponible");
        }

        // VALIDATION 2: Vérifier que la personne existe dans la base de données
        Optional<Personne> personne = personneDAO.findById(location.getId_personne());
        if (!personne.isPresent()) {
            throw new IllegalArgumentException("Personne non trouvée");
        }

        // INITIALISATION: Définir l'état initial de la location
        location.setEtat("EN_COURS");

        // PERSISTANCE: Créer la location (via le DAO)
        // Cette opération met également à jour l'état de la voiture à "LOUEE"
        Long id = locationDAO.save(location);

        // MISE À JOUR STATISTIQUES: Incrémenter le compteur de locations de la personne
        Personne p = personne.get();
        p.setNbloc(p.getNbloc() + 1);
        personneDAO.update(p);

        return id;
    }

    /**
     * Termine une location existante.
     * Vérifie d'abord que la location existe, puis délègue au DAO pour:
     * 1. Changer l'état de la location à "TERMINEE"
     * 2. Remettre la voiture à l'état "DISPONIBLE"
     * 
     * @param id_location ID de la location à terminer
     * @throws IllegalArgumentException si la location n'existe pas
     */
    public void terminerLocation(Long id_location) {
        // Vérifier que la location existe avant de tenter de la terminer
        Optional<Location> location = locationDAO.findById(id_location);
        if (!location.isPresent()) {
            throw new IllegalArgumentException("Location non trouvée");
        }

        // Terminer la location et rendre la voiture disponible
        locationDAO.terminerLocation(id_location);
    }

    /**
     * Vérifie si une voiture est actuellement en location.
     * Simple délégation au DAO correspondant.
     * 
     * @param mat Immatriculation de la voiture à vérifier
     * @return true si la voiture est actuellement louée, false sinon
     */
    public boolean isVoitureLouee(String mat) {
        return locationDAO.isVoitureLouee(mat);
    }

    /**
     * Calcule le montant total d'une location en fonction de la voiture et de la durée.
     * Formule: prix journalier * nombre de jours
     * 
     * @param mat Immatriculation de la voiture
     * @param duree Durée de la location en jours
     * @return Montant total de la location, ou 0 si la voiture n'est pas trouvée
     */
    public double calculerMontantLocation(String mat, int duree) {
        // Récupérer les informations de la voiture pour connaître son prix journalier
        Optional<Voiture> voiture = voitureDAO.findByMat(mat);
        if (voiture.isPresent()) {
            // Calcul simple: prix journalier * nombre de jours
            return voiture.get().getPrix() * duree;
        }
        return 0; // Retourne 0 si la voiture n'est pas trouvée
    }

    /**
     * Recherche une location spécifique par son ID.
     * Simple délégation au DAO correspondant.
     * 
     * @param id_location ID de la location à rechercher
     * @return Optional contenant la location si trouvée, vide sinon
     */
    public Optional<Location> findById(Long id_location) {
        return locationDAO.findById(id_location);
    }
} 