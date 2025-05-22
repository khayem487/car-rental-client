package com.carrental.client.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    @Autowired
    private DataSource dataSource;

    // Bean pour tester la connexion à la base de données au démarrage
    @Bean
    public CommandLineRunner testDatabaseConnection() {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("=====================================================");
                System.out.println("Base de données connectée avec succès !");
                System.out.println("URL: " + connection.getMetaData().getURL());
                System.out.println("Utilisateur: " + connection.getMetaData().getUserName());
                System.out.println("=====================================================");
            } catch (SQLException e) {
                System.err.println("Erreur de connexion à la base de données:");
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        };
    }

    // Bean pour exécuter des requêtes SQL initiales si nécessaire
    @Bean
    public CommandLineRunner testSimpleQuery(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Vérifier si la table personne existe
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM personne", Integer.class);
                System.out.println("Nombre de personnes dans la base de données: " + count);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'exécution de la requête SQL:");
                System.err.println(e.getMessage());
                System.err.println("Assurez-vous que les tables sont créées en exécutant le script schema_and_data.sql");
            }
        };
    }
} 