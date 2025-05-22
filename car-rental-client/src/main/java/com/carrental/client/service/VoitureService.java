package com.carrental.client.service;

import com.carrental.client.dao.VoitureDAO;
import com.carrental.client.model.Voiture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des voitures.
 * 
 * Cette classe implémente la logique métier liée aux voitures:
 * - Récupération des voitures avec différents filtres
 * - Vérification de la disponibilité des voitures
 * - Gestion des statistiques et des voitures populaires
 * 
 * Elle sert d'intermédiaire entre les contrôleurs et la couche d'accès aux données (DAO),
 * en fournissant des méthodes orientées cas d'utilisation pour les opérations sur les voitures.
 * Une particularité importante est que ce service filtre systématiquement les voitures
 * en panne pour les clients.
 */
@Service
public class VoitureService {

    /**
     * Accès aux données des voitures
     */
    private final VoitureDAO voitureDAO;

    /**
     * Constructeur avec injection de dépendance pour le DAO des voitures.
     * 
     * @param voitureDAO DAO pour l'accès aux données des voitures
     */
    @Autowired
    public VoitureService(VoitureDAO voitureDAO) {
        this.voitureDAO = voitureDAO;
    }

    /**
     * Récupère toutes les voitures visibles par les clients.
     * Cette méthode exclut automatiquement les voitures en PANNE,
     * qui ne devraient pas être visibles pour les clients.
     * 
     * @return Liste de toutes les voitures disponibles et louées (mais pas en panne)
     */
    public List<Voiture> getAllAvailableVoitures() {
        return voitureDAO.findAllAvailable();
    }

    /**
     * Récupère uniquement les voitures actuellement disponibles à la location.
     * Ces voitures ont l'état "DISPONIBLE" et peuvent être immédiatement louées.
     * 
     * @return Liste des voitures avec l'état DISPONIBLE
     */
    public List<Voiture> getVoituresDisponibles() {
        return voitureDAO.findByDisponible();
    }

    /**
     * Récupère uniquement les voitures actuellement en location.
     * Ces voitures ont l'état "LOUEE" et ne sont pas disponibles pour une nouvelle location.
     * 
     * @return Liste des voitures avec l'état LOUEE
     */
    public List<Voiture> getVoituresLouees() {
        return voitureDAO.findByLouee();
    }

    /**
     * Recherche une voiture spécifique par son immatriculation (identifiant unique).
     * 
     * @param mat Immatriculation de la voiture à rechercher
     * @return Optional contenant la voiture si trouvée, vide sinon
     */
    public Optional<Voiture> findByMat(String mat) {
        return voitureDAO.findByMat(mat);
    }

    /**
     * Récupère les trois voitures les plus fréquemment louées.
     * Utilisé pour afficher les voitures populaires sur la page d'accueil
     * ou pour des recommandations aux clients.
     * 
     * @return Liste des 3 voitures les plus louées (triées par nombre de locations décroissant)
     */
    public List<Voiture> getMostRentedVoitures() {
        return voitureDAO.findMostRented();
    }

    /**
     * Filtre les voitures par marque (recherche partielle).
     * Permet une recherche insensible à la casse et partielle (contient).
     * 
     * @param marque Marque ou partie de marque à rechercher
     * @return Liste des voitures dont la marque contient le texte recherché
     */
    public List<Voiture> filterByMarque(String marque) {
        return voitureDAO.findByMarque(marque);
    }

    /**
     * Filtre les voitures par nombre de places.
     * Utile pour les clients recherchant un véhicule adapté à leurs besoins.
     * 
     * @param nbplace Nombre exact de places recherché
     * @return Liste des voitures ayant exactement ce nombre de places
     */
    public List<Voiture> filterByNbPlace(int nbplace) {
        return voitureDAO.findByNbPlace(nbplace);
    }

    /**
     * Filtre les voitures par prix maximum.
     * Permet aux clients de trouver des voitures dans leur budget.
     * 
     * @param prixMax Prix journalier maximum recherché
     * @return Liste des voitures dont le prix est inférieur ou égal au prix spécifié
     */
    public List<Voiture> filterByPrixMax(double prixMax) {
        return voitureDAO.findByPrixMax(prixMax);
    }

    /**
     * Vérifie si une voiture spécifique est disponible pour la location.
     * Une voiture est considérée disponible si:
     * 1. Elle existe dans la base de données
     * 2. Son état est exactement "DISPONIBLE"
     * 
     * @param mat Immatriculation de la voiture à vérifier
     * @return true si la voiture existe et est disponible, false sinon
     */
    public boolean isVoitureDisponible(String mat) {
        Optional<Voiture> voiture = voitureDAO.findByMat(mat);
        return voiture.isPresent() && "DISPONIBLE".equals(voiture.get().getDisp());
    }
} 