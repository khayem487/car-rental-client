package com.carrental.client;

import com.carrental.client.dao.LocationDAO;
import com.carrental.client.dao.VoitureDAO;
import com.carrental.client.model.Location;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class LocationQueryTest {
    @Test void bookingAssociationsAndOwnerFiltersUseOneQueryInOracleMode() {
        verifyBookingReads("Oracle");
    }
    @Test void bookingAssociationsAndOwnerFiltersUseOneQueryInPostgresMode() {
        verifyBookingReads("PostgreSQL");
    }

    private void verifyBookingReads(String mode) {
        AtomicInteger statements = new AtomicInteger();
        DriverManagerDataSource database = new DriverManagerDataSource(
            "jdbc:h2:mem:joined-" + UUID.randomUUID() + ";MODE=" + mode + ";DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(new AbstractDataSource() {
            @Override public Connection getConnection() throws SQLException { return counted(database.getConnection(), statements); }
            @Override public Connection getConnection(String username, String password) throws SQLException {
                return counted(database.getConnection(username, password), statements);
            }
        });
        try {
            jdbc.execute("CREATE TABLE personne(id BIGINT PRIMARY KEY,cin VARCHAR(20),nom VARCHAR(50),prenom VARCHAR(50),numero VARCHAR(20),npermis VARCHAR(20),nbloc INTEGER,login VARCHAR(50),passwd VARCHAR(100),role VARCHAR(20))");
            jdbc.execute("CREATE TABLE voiture(mat VARCHAR(20) PRIMARY KEY,marque VARCHAR(50),model VARCHAR(50),nbplace INTEGER,disp VARCHAR(20),nbloc INTEGER,prix DECIMAL(10,2),image_path VARCHAR(255))");
            jdbc.execute("CREATE TABLE location(id_location BIGINT PRIMARY KEY,id_personne BIGINT,mat VARCHAR(20),dteDeb DATE,duree INTEGER,cheque VARCHAR(50),etat VARCHAR(20))");
            jdbc.update("INSERT INTO personne VALUES (101,'D101','Martin','Sophie','DEMO','P101',9,'sophie.test','encoded','CLIENT')");
            jdbc.update("INSERT INTO personne VALUES (102,'D102','Autre','Client','DEMO','P102',2,'autre.test','encoded','CLIENT')");
            jdbc.update("INSERT INTO voiture VALUES ('A1','Test','Premier',5,'LOUEE',3,65.25,'first.svg')");
            jdbc.update("INSERT INTO voiture VALUES ('A2','Test','Second',7,'DISPONIBLE',4,42.50,'second.svg')");
            jdbc.update("INSERT INTO location VALUES (1,101,'A1',DATE '2026-10-26',2,'FIRST','EN_COURS')");
            jdbc.update("INSERT INTO location VALUES (2,101,'A2',DATE '2026-10-20',3,'SECOND','TERMINEE')");
            jdbc.update("INSERT INTO location VALUES (3,102,'A2',DATE '2026-10-19',1,'OTHER','TERMINEE')");
            LocationDAO dao = new LocationDAO(jdbc, new VoitureDAO(jdbc));

            statements.set(0);
            var history = dao.findByPersonneId(101L);
            assertThat(statements).hasValue(1);
            assertThat(history).extracting(Location::getId_location).containsExactlyInAnyOrder(1L, 2L);
            assertThat(history).allSatisfy(location -> {
                assertThat(location.getPersonne().getId()).isEqualTo(101L);
                assertThat(location.getPersonne().getLogin()).isEqualTo("sophie.test");
                assertThat(location.getPersonne().getNbloc()).isEqualTo(9);
                assertThat(location.getVoiture().getMat()).isEqualTo(location.getMat());
            });

            statements.set(0);
            var active = dao.findActiveByPersonneId(101L);
            assertThat(statements).hasValue(1);
            assertThat(active).hasSize(1);
            assertThat(active.get(0).getMontantTotal()).isEqualTo(130.50);
            assertThat(active.get(0).getVoiture().getImagePath()).isEqualTo("first.svg");
            assertThat(active.get(0).getPersonne().getPrenom()).isEqualTo("Sophie");

            statements.set(0);
            Location detail = dao.findById(2L).orElseThrow();
            assertThat(statements).hasValue(1);
            assertThat(detail.getEtat()).isEqualTo("TERMINEE");
            assertThat(detail.getCheque()).isEqualTo("SECOND");
            assertThat(detail.getMontantTotal()).isEqualTo(127.50);
            assertThat(detail.getVoiture().getNbplace()).isEqualTo(7);
            assertThat(detail.getPersonne().getNom()).isEqualTo("Martin");
            assertThat(dao.findById(999L)).isEmpty();
            assertThat(dao.findByPersonneId(999L)).isEmpty();
        } finally {
            jdbc.execute("SHUTDOWN");
        }
    }

    private static Connection counted(Connection target, AtomicInteger statements) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("prepareStatement")) statements.incrementAndGet();
                try { return method.invoke(target, args); }
                catch (InvocationTargetException failure) { throw failure.getCause(); }
            });
    }
}
