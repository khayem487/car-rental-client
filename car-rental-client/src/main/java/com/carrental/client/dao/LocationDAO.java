package com.carrental.client.dao;

import com.carrental.client.model.Location;
import com.carrental.client.model.Personne;
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
    
    // Load display associations in the same query instead of two extra trips per booking.
    private static final String WITH_DETAILS = "SELECT l.*, "
        + "v.mat AS v_mat, v.marque AS v_marque, v.model AS v_model, v.nbplace AS v_nbplace, "
        + "v.disp AS v_disp, v.nbloc AS v_nbloc, v.prix AS v_prix, v.image_path AS v_image_path, "
        + "p.id AS p_id, p.cin AS p_cin, p.nom AS p_nom, p.prenom AS p_prenom, "
        + "p.numero AS p_numero, p.npermis AS p_npermis, p.nbloc AS p_nbloc, "
        + "p.login AS p_login, p.passwd AS p_passwd, p.role AS p_role "
        + "FROM location l LEFT JOIN voiture v ON v.mat = l.mat "
        + "LEFT JOIN personne p ON p.id = l.id_personne ";

    /**
     * Constructeur avec injection des dépendances.
     * 
     * @param jdbcTemplate JdbcTemplate pour l'accès à la base de données
     * @param voitureDAO DAO pour accéder aux voitures
     */
    @Autowired
    public LocationDAO(JdbcTemplate jdbcTemplate, VoitureDAO voitureDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.voitureDAO = voitureDAO;
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
            if (rs.getString("v_mat") != null) {
                location.setVoiture(new Voiture(rs.getString("v_mat"), rs.getString("v_marque"),
                    rs.getString("v_model"), rs.getInt("v_nbplace"), rs.getString("v_disp"),
                    rs.getInt("v_nbloc"), rs.getDouble("v_prix"), rs.getString("v_image_path")));
            }
            if (rs.getObject("p_id") != null) {
                location.setPersonne(new Personne(rs.getLong("p_id"), rs.getString("p_cin"),
                    rs.getString("p_nom"), rs.getString("p_prenom"), rs.getString("p_numero"),
                    rs.getString("p_npermis"), rs.getInt("p_nbloc"), rs.getString("p_login"),
                    rs.getString("p_passwd"), rs.getString("p_role")));
            }
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
        return jdbcTemplate.query(WITH_DETAILS + "WHERE l.id_personne = ?", new LocationRowMapper(), id_personne);
    }

    /**
     * Récupère uniquement les locations en cours (actives) d'un utilisateur.
     * 
     * @param id_personne ID de l'utilisateur
     * @return Liste des locations en cours de l'utilisateur
     */
    public List<Location> findActiveByPersonneId(Long id_personne) {
        return jdbcTemplate.query(WITH_DETAILS + "WHERE l.id_personne = ? AND l.etat = 'EN_COURS'",
            new LocationRowMapper(), id_personne);
    }

    /**
     * Recherche une location spécifique par son identifiant.
     * Charge également les détails de la voiture et de la personne associées.
     * 
     * @param id_location ID de la location à rechercher
     * @return Optional contenant la location si trouvée, vide sinon
     */
    public Optional<Location> findById(Long id_location) {
        return jdbcTemplate.query(WITH_DETAILS + "WHERE l.id_location = ?", new LocationRowMapper(), id_location)
            .stream().findFirst();
    }

    /**
     * Crée une nouvelle location dans la base de données.
     * Met également à jour l'état de la voiture associée.
     * 
     * @param location Objet Location à enregistrer
     * @return ID généré pour la nouvelle location
     */
    public Long save(Location location) {
        // Claim the vehicle atomically; the service transaction rolls it back if insertion fails.
        if (jdbcTemplate.update("UPDATE voiture SET disp = 'LOUEE' WHERE mat = ? AND disp = 'DISPONIBLE'", location.getMat()) != 1)
            throw new IllegalArgumentException("Cette voiture vient d'être réservée. Choisissez un autre véhicule.");
        return GeneratedIds.insert(jdbcTemplate, "location", "id_location", "seq_location",
            "id_personne, mat, dteDeb, duree, cheque, etat", location.getId_personne(), location.getMat(),
            new java.sql.Date(location.getDteDeb().getTime()), location.getDuree(), location.getCheque(), location.getEtat());
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
        String updateSql = "UPDATE location SET etat = 'TERMINEE' WHERE id_location = ? AND etat = 'EN_COURS'";
        if (jdbcTemplate.update(updateSql, id_location) != 1)
            throw new IllegalArgumentException("Cette location est déjà terminée.");
        
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
