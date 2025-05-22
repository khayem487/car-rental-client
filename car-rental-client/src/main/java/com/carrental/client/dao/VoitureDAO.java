package com.carrental.client.dao;

import com.carrental.client.model.Voiture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Classe d'accès aux données pour l'entité Voiture.
 * Gère toutes les opérations de lecture et modification des données
 * de voitures dans la base de données.
 */
@Repository
public class VoitureDAO {

    /**
     * JdbcTemplate utilisé pour exécuter les requêtes SQL.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructeur avec injection de dépendance de JdbcTemplate.
     * 
     * @param jdbcTemplate Instance de JdbcTemplate configurée par Spring
     */
    @Autowired
    public VoitureDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper pour convertir les résultats de requêtes SQL en objets Voiture.
     * Mappe chaque colonne de la table voiture aux propriétés de l'objet Voiture.
     */
    private static final class VoitureRowMapper implements RowMapper<Voiture> {
        @Override
        public Voiture mapRow(ResultSet rs, int rowNum) throws SQLException {
            // Création et hydratation d'un nouvel objet Voiture
            Voiture voiture = new Voiture();
            voiture.setMat(rs.getString("mat"));           // Immatriculation (clé primaire)
            voiture.setMarque(rs.getString("marque"));     // Marque du véhicule
            voiture.setModel(rs.getString("model"));       // Modèle du véhicule
            voiture.setNbplace(rs.getInt("nbplace"));      // Nombre de places
            voiture.setDisp(rs.getString("disp"));         // État de disponibilité (DISPONIBLE, LOUEE, PANNE)
            voiture.setNbloc(rs.getInt("nbloc"));          // Nombre de fois que la voiture a été louée
            voiture.setPrix(rs.getDouble("prix"));         // Prix de location par jour
            voiture.setImagePath(rs.getString("image_path")); // Chemin vers l'image de la voiture
            return voiture;
        }
    }

    /**
     * Récupère toutes les voitures qui ne sont pas en panne.
     * Utilisée pour afficher les voitures aux clients (qui ne devraient pas voir les voitures en panne).
     * 
     * @return Liste de toutes les voitures disponibles ou louées (pas en panne)
     */
    public List<Voiture> findAllAvailable() {
        // Requête qui exclut les voitures en état PANNE
        String sql = "SELECT * FROM voiture WHERE disp != 'PANNE'";
        return jdbcTemplate.query(sql, new VoitureRowMapper());
    }

    /**
     * Récupère uniquement les voitures disponibles à la location.
     * 
     * @return Liste des voitures avec l'état DISPONIBLE
     */
    public List<Voiture> findByDisponible() {
        String sql = "SELECT * FROM voiture WHERE disp = 'DISPONIBLE'";
        return jdbcTemplate.query(sql, new VoitureRowMapper());
    }
    
    /**
     * Récupère uniquement les voitures actuellement en location.
     * 
     * @return Liste des voitures avec l'état LOUEE
     */
    public List<Voiture> findByLouee() {
        String sql = "SELECT * FROM voiture WHERE disp = 'LOUEE'";
        return jdbcTemplate.query(sql, new VoitureRowMapper());
    }

    /**
     * Recherche une voiture par son immatriculation (clé primaire).
     * 
     * @param mat Immatriculation de la voiture à rechercher
     * @return Optional contenant la voiture si trouvée, vide sinon
     */
    public Optional<Voiture> findByMat(String mat) {
        // Requête paramétrée pour éviter les injections SQL
        String sql = "SELECT * FROM voiture WHERE mat = ?";
        List<Voiture> voitures = jdbcTemplate.query(sql, new VoitureRowMapper(), mat);
        return voitures.isEmpty() ? Optional.empty() : Optional.of(voitures.get(0));
    }

    /**
     * Met à jour l'état de disponibilité d'une voiture.
     * Utilisée lors de la location ou du retour d'une voiture.
     * 
     * @param mat Immatriculation de la voiture à mettre à jour
     * @param disp Nouvel état (DISPONIBLE, LOUEE)
     */
    public void updateDisp(String mat, String disp) {
        String sql = "UPDATE voiture SET disp = ? WHERE mat = ?";
        jdbcTemplate.update(sql, disp, mat);
    }

    /**
     * Recherche les trois voitures les plus louées.
     * Utilise une requête SQL complexe avec jointure pour calculer le nombre
     * de locations pour chaque voiture.
     * 
     * @return Liste des 3 voitures les plus louées
     */
    public List<Voiture> findMostRented() {
        // Requête SQL avancée avec:
        // - Jointure entre voiture et location
        // - Agrégation (COUNT) pour compter les locations
        // - Regroupement par voiture
        // - Tri par nombre de locations décroissant
        // - Limitation aux 3 premiers résultats
        String sql = "SELECT v.*, COUNT(l.mat) as nb_locations FROM voiture v " +
                     "JOIN location l ON v.mat = l.mat " +
                     "GROUP BY v.mat, v.marque, v.model, v.nbplace, v.disp, v.nbloc, v.prix, v.image_path " +
                     "ORDER BY nb_locations DESC " +
                     "FETCH FIRST 3 ROWS ONLY";
        return jdbcTemplate.query(sql, new VoitureRowMapper());
    }

    /**
     * Filtre les voitures par marque (recherche partielle).
     * 
     * @param marque Marque à rechercher (recherche avec LIKE)
     * @return Liste des voitures correspondant à la marque recherchée
     */
    public List<Voiture> findByMarque(String marque) {
        // Utilisation de % pour permettre une recherche partielle et insensible à la casse
        String sql = "SELECT * FROM voiture WHERE marque LIKE ? AND disp != 'PANNE'";
        return jdbcTemplate.query(sql, new VoitureRowMapper(), "%" + marque + "%");
    }

    /**
     * Filtre les voitures par nombre de places.
     * 
     * @param nbplace Nombre de places recherché
     * @return Liste des voitures ayant exactement ce nombre de places
     */
    public List<Voiture> findByNbPlace(int nbplace) {
        String sql = "SELECT * FROM voiture WHERE nbplace = ? AND disp != 'PANNE'";
        return jdbcTemplate.query(sql, new VoitureRowMapper(), nbplace);
    }

    /**
     * Filtre les voitures par prix maximum.
     * Retourne les voitures dont le prix est inférieur ou égal au prix spécifié.
     * 
     * @param prixMax Prix maximum recherché
     * @return Liste des voitures dont le prix est inférieur ou égal au prix spécifié
     */
    public List<Voiture> findByPrixMax(double prixMax) {
        String sql = "SELECT * FROM voiture WHERE prix <= ? AND disp != 'PANNE'";
        return jdbcTemplate.query(sql, new VoitureRowMapper(), prixMax);
    }
} 