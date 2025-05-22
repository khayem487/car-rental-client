package com.carrental.client.dao;

import com.carrental.client.model.Personne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Classe d'accès aux données pour l'entité Personne.
 * Cette classe gère toutes les opérations CRUD (Create, Read, Update, Delete)
 * liées aux utilisateurs dans la base de données.
 * 
 * @Repository: Indique à Spring que cette classe est un composant de la couche d'accès aux données
 * et permet l'injection automatique de cette classe dans d'autres composants.
 */
@Repository 
public class PersonneDAO {

    /**
     * JdbcTemplate: Outil fourni par Spring qui simplifie les opérations JDBC
     * et gère automatiquement l'ouverture/fermeture des connexions et les exceptions SQL.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructeur avec injection de dépendance par Spring.
     * @Autowired: Indique à Spring d'injecter automatiquement un JdbcTemplate configuré.
     * 
     * @param jdbcTemplate Instance de JdbcTemplate configurée par Spring
     */
    @Autowired
    public PersonneDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Classe interne qui implémente RowMapper pour convertir les résultats SQL en objets Personne.
     * Cette classe est utilisée par JdbcTemplate pour transformer chaque ligne retournée
     * par une requête SQL en un objet Java Personne.
     */
    private static final class PersonneRowMapper implements RowMapper<Personne> {
        @Override
        public Personne mapRow(ResultSet rs, int rowNum) throws SQLException {
            // Création d'un nouvel objet Personne
            Personne personne = new Personne();
            
            // Extraction des données depuis le ResultSet et affectation à l'objet Personne
            personne.setId(rs.getLong("id"));
            personne.setCin(rs.getString("cin"));
            personne.setNom(rs.getString("nom"));
            personne.setPrenom(rs.getString("prenom"));
            personne.setNumero(rs.getString("numero"));
            personne.setNpermis(rs.getString("npermis"));
            personne.setNbloc(rs.getInt("nbloc"));
            personne.setLogin(rs.getString("login"));
            personne.setPasswd(rs.getString("passwd"));
            personne.setRole(rs.getString("role"));
            
            return personne;
        }
    }

    /**
     * Recherche une personne par son login.
     * 
     * @param login Login de la personne à rechercher
     * @return Optional contenant la personne si trouvée, vide sinon
     */
    public Optional<Personne> findByLogin(String login) {
        // Requête SQL avec paramètre préparé (?) pour éviter les injections SQL
        String sql = "SELECT * FROM personne WHERE login = ?";
        
        // Exécution de la requête avec conversion des résultats via PersonneRowMapper
        List<Personne> personnes = jdbcTemplate.query(sql, new PersonneRowMapper(), login);
        
        // Transformation du résultat en Optional (présent si trouvé, vide sinon)
        return personnes.isEmpty() ? Optional.empty() : Optional.of(personnes.get(0));
    }

    /**
     * Recherche une personne par son identifiant.
     * 
     * @param id Identifiant de la personne à rechercher
     * @return Optional contenant la personne si trouvée, vide sinon
     */
    public Optional<Personne> findById(Long id) {
        // Requête SQL avec paramètre préparé
        String sql = "SELECT * FROM personne WHERE id = ?";
        
        // Exécution et conversion des résultats
        List<Personne> personnes = jdbcTemplate.query(sql, new PersonneRowMapper(), id);
        
        return personnes.isEmpty() ? Optional.empty() : Optional.of(personnes.get(0));
    }

    /**
     * Authentifie un utilisateur en vérifiant ses identifiants.
     * 
     * @param login Login de l'utilisateur
     * @param passwd Mot de passe de l'utilisateur
     * @return Optional contenant la personne si authentification réussie, vide sinon
     */
    public Optional<Personne> authentifier(String login, String passwd) {
        // Requête SQL avec deux paramètres préparés pour vérifier login et mot de passe
        String sql = "SELECT * FROM personne WHERE login = ? AND passwd = ?";
        
        // Exécution de la requête avec les deux paramètres
        List<Personne> personnes = jdbcTemplate.query(sql, new PersonneRowMapper(), login, passwd);
        
        return personnes.isEmpty() ? Optional.empty() : Optional.of(personnes.get(0));
    }

    /**
     * Enregistre un nouvel utilisateur dans la base de données.
     * 
     * @param personne Objet Personne à enregistrer
     * @return Identifiant généré pour le nouvel utilisateur
     */
    public Long save(Personne personne) {
        // Requête SQL d'insertion avec séquence Oracle pour l'ID
        String sql = "INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role) " +
                     "VALUES (seq_personne.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // Exécution de la requête d'insertion avec tous les paramètres dans l'ordre des ?
        jdbcTemplate.update(sql, 
                 personne.getCin(),
                 personne.getNom(), 
                 personne.getPrenom(), 
                 personne.getNumero(),
                 personne.getNpermis(), 
                 personne.getNbloc(), 
                 personne.getLogin(), 
                 personne.getPasswd(), 
                 personne.getRole());
        
        // Récupération de l'ID généré par la séquence Oracle
        String idSql = "SELECT seq_personne.CURRVAL FROM dual";
        return jdbcTemplate.queryForObject(idSql, Long.class);
    }

    /**
     * Met à jour les informations d'un utilisateur existant.
     * 
     * @param personne Objet Personne contenant les nouvelles informations
     */
    public void update(Personne personne) {
        // Requête SQL de mise à jour
        String sql = "UPDATE personne SET cin = ?, nom = ?, prenom = ?, numero = ?, npermis = ?, " +
                     "nbloc = ?, login = ?, passwd = ?, role = ? WHERE id = ?";
        
        // Exécution de la requête avec tous les paramètres + l'ID pour la clause WHERE
        jdbcTemplate.update(sql, 
                 personne.getCin(),
                 personne.getNom(), 
                 personne.getPrenom(), 
                 personne.getNumero(),
                 personne.getNpermis(), 
                 personne.getNbloc(), 
                 personne.getLogin(), 
                 personne.getPasswd(), 
                 personne.getRole(),
                 personne.getId());
    }

    /**
     * Vérifie si un login est déjà utilisé par un autre utilisateur.
     * 
     * @param login Login à vérifier
     * @return true si le login existe déjà, false sinon
     */
    public boolean loginExists(String login) {
        // Requête SQL qui compte le nombre d'occurrences d'un login
        String sql = "SELECT COUNT(*) FROM personne WHERE login = ?";
        
        // Exécution et conversion du résultat en Integer
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, login);
        
        // Retourne true si le compteur est supérieur à zéro
        return count != null && count > 0;
    }

    /**
     * Vérifie si un numéro CIN est déjà enregistré dans la base de données.
     * 
     * @param cin Numéro CIN à vérifier
     * @return true si le CIN existe déjà, false sinon
     */
    public boolean cinExists(String cin) {
        // Requête SQL qui compte le nombre d'occurrences d'un CIN
        String sql = "SELECT COUNT(*) FROM personne WHERE cin = ?";
        
        // Exécution et conversion du résultat
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, cin);
        
        return count != null && count > 0;
    }
} 