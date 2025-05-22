package com.carrental.client.dao;

import com.carrental.client.model.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Classe d'accès aux données pour l'entité Location.
 * Cette classe gère les opérations de lecture, création et modification 
 * des locations dans la base de données.
 * Elle coordonne également les mises à jour d'état des voitures associées.
 */
@Repository
public class LocationDAO {

    /** JdbcTemplate pour l'exécution des requêtes SQL */
    private final JdbcTemplate jdbcTemplate;
    
    /** DAO pour accéder aux données des voitures (dépendance) */
    private final VoitureDAO voitureDAO;
    
    /** DAO pour accéder aux données des personnes (dépendance) */
    private final PersonneDAO personneDAO;

    /**
     * Constructeur avec injection des dépendances.
     * 
     * @param jdbcTemplate JdbcTemplate pour l'accès à la base de données
     * @param voitureDAO DAO pour accéder aux voitures
     * @param personneDAO DAO pour accéder aux personnes
     */
    @Autowired
    public LocationDAO(JdbcTemplate jdbcTemplate, VoitureDAO voitureDAO, PersonneDAO personneDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.voitureDAO = voitureDAO;
        this.personneDAO = personneDAO;
    }

    /**
     * RowMapper pour convertir les résultats SQL en objets Location.
     * Mappe chaque colonne de la table location aux propriétés de l'objet Location.
     */
    private static final class LocationRowMapper implements RowMapper<Location> {
        @Override
        public Location mapRow(ResultSet rs, int rowNum) throws SQLException {
            Location location = new Location();
            location.setId_location(rs.getLong("id_location"));     // ID de la location (clé primaire)
            location.setId_personne(rs.getLong("id_personne"));     // ID de la personne (clé étrangère)
            location.setMat(rs.getString("mat"));                   // Immatriculation de la voiture (clé étrangère)
            location.setDteDeb(rs.getDate("dteDeb"));               // Date de début de location
            location.setDuree(rs.getInt("duree"));                  // Durée en jours
            location.setCheque(rs.getString("cheque"));             // Référence du paiement
            location.setEtat(rs.getString("etat"));                 // État (EN_COURS, TERMINEE)
            return location;
        }
    }

    /**
     * Récupère toutes les locations d'un utilisateur spécifique.
     * Pour chaque location, charge également les détails de la voiture et de la personne associées.
     * 
     * @param id_personne ID de l'utilisateur dont on veut récupérer les locations
     * @return Liste de toutes les locations de l'utilisateur
     */
    public List<Location> findByPersonneId(Long id_personne) {
        // Requête SQL filtrée par l'ID de la personne
        String sql = "SELECT * FROM location WHERE id_personne = ?";
        List<Location> locations = jdbcTemplate.query(sql, new LocationRowMapper(), id_personne);
        
        // Enrichissement des objets Location avec les données associées (voiture et personne)
        for (Location location : locations) {
            // Chargement de la voiture associée si elle existe
            voitureDAO.findByMat(location.getMat()).ifPresent(location::setVoiture);
            // Chargement de la personne associée si elle existe
            personneDAO.findById(location.getId_personne()).ifPresent(location::setPersonne);
        }
        
        return locations;
    }

    /**
     * Récupère uniquement les locations en cours (actives) d'un utilisateur.
     * 
     * @param id_personne ID de l'utilisateur
     * @return Liste des locations en cours de l'utilisateur
     */
    public List<Location> findActiveByPersonneId(Long id_personne) {
        // Requête SQL filtrée par ID personne ET état de la location
        String sql = "SELECT * FROM location WHERE id_personne = ? AND etat = 'EN_COURS'";
        List<Location> locations = jdbcTemplate.query(sql, new LocationRowMapper(), id_personne);
        
        // Enrichissement des objets avec les détails de voiture et personne
        for (Location location : locations) {
            voitureDAO.findByMat(location.getMat()).ifPresent(location::setVoiture);
            personneDAO.findById(location.getId_personne()).ifPresent(location::setPersonne);
        }
        
        return locations;
    }

    /**
     * Recherche une location spécifique par son identifiant.
     * Charge également les détails de la voiture et de la personne associées.
     * 
     * @param id_location ID de la location à rechercher
     * @return Optional contenant la location si trouvée, vide sinon
     */
    public Optional<Location> findById(Long id_location) {
        // Requête SQL paramétrée par l'ID de la location
        String sql = "SELECT * FROM location WHERE id_location = ?";
        List<Location> locations = jdbcTemplate.query(sql, new LocationRowMapper(), id_location);
        
        if (locations.isEmpty()) {
            return Optional.empty();
        }
        
        // Enrichissement de l'objet Location avec les détails associés
        Location location = locations.get(0);
        voitureDAO.findByMat(location.getMat()).ifPresent(location::setVoiture);
        personneDAO.findById(location.getId_personne()).ifPresent(location::setPersonne);
        
        return Optional.of(location);
    }

    /**
     * Crée une nouvelle location dans la base de données.
     * Met également à jour l'état de la voiture associée.
     * 
     * @param location Objet Location à enregistrer
     * @return ID généré pour la nouvelle location
     */
    public Long save(Location location) {
        // Requête SQL d'insertion avec séquence Oracle pour l'ID
        String sql = "INSERT INTO location (id_location, id_personne, mat, dteDeb, duree, cheque, etat) " +
                     "VALUES (seq_location.NEXTVAL, ?, ?, ?, ?, ?, ?)";
        
        // Exécution de la requête d'insertion avec conversion de la date Java en date SQL
        jdbcTemplate.update(sql, 
                location.getId_personne(),
                location.getMat(),
                new java.sql.Date(location.getDteDeb().getTime()),  // Conversion java.util.Date → java.sql.Date
                location.getDuree(),
                location.getCheque(),
                location.getEtat());
        
        // Effet secondaire: mise à jour de l'état de la voiture à LOUEE
        voitureDAO.updateDisp(location.getMat(), "LOUEE");
        
        // Récupération de l'ID généré par la séquence Oracle
        String idSql = "SELECT seq_location.CURRVAL FROM dual";
        return jdbcTemplate.queryForObject(idSql, Long.class);
    }

    /**
     * Termine une location existante (change son état à TERMINEE).
     * Met également à jour l'état de la voiture associée à DISPONIBLE.
     * 
     * @param id_location ID de la location à terminer
     */
    public void terminerLocation(Long id_location) {
        // 1. Récupération de l'immatriculation de la voiture associée à la location
        String sql = "SELECT mat FROM location WHERE id_location = ?";
        String mat = jdbcTemplate.queryForObject(sql, String.class, id_location);
        
        // 2. Mise à jour de l'état de la location à TERMINEE
        String updateSql = "UPDATE location SET etat = 'TERMINEE' WHERE id_location = ?";
        jdbcTemplate.update(updateSql, id_location);
        
        // 3. Mise à jour de l'état de la voiture à DISPONIBLE
        voitureDAO.updateDisp(mat, "DISPONIBLE");
    }

    /**
     * Vérifie si une voiture est actuellement en location.
     * 
     * @param mat Immatriculation de la voiture à vérifier
     * @return true si la voiture est en cours de location, false sinon
     */
    public boolean isVoitureLouee(String mat) {
        // Requête SQL qui compte les locations actives pour cette voiture
        String sql = "SELECT COUNT(*) FROM location WHERE mat = ? AND etat = 'EN_COURS'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mat);
        return count != null && count > 0;
    }
} 