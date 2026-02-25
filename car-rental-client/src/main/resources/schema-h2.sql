DROP TABLE IF EXISTS location;
DROP TABLE IF EXISTS voiture;
DROP TABLE IF EXISTS personne;
DROP SEQUENCE IF EXISTS seq_personne;
DROP SEQUENCE IF EXISTS seq_location;

CREATE TABLE personne (
    id BIGINT PRIMARY KEY,
    cin VARCHAR(20) UNIQUE NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    npermis VARCHAR(20) NOT NULL,
    nbloc INT DEFAULT 0,
    login VARCHAR(50) UNIQUE NOT NULL,
    passwd VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE voiture (
    mat VARCHAR(20) PRIMARY KEY,
    marque VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    nbplace INT NOT NULL,
    disp VARCHAR(20) NOT NULL,
    nbloc INT DEFAULT 0,
    prix DOUBLE NOT NULL,
    image_path VARCHAR(255) DEFAULT 'default.jpg'
);

CREATE TABLE location (
    id_location BIGINT PRIMARY KEY,
    id_personne BIGINT NOT NULL,
    mat VARCHAR(20) NOT NULL,
    dteDeb DATE NOT NULL,
    duree INT NOT NULL,
    cheque VARCHAR(50) NOT NULL,
    etat VARCHAR(20) NOT NULL,
    FOREIGN KEY (id_personne) REFERENCES personne(id),
    FOREIGN KEY (mat) REFERENCES voiture(mat)
);

CREATE INDEX idx_personne_login ON personne(login);
CREATE INDEX idx_voiture_disp ON voiture(disp);
CREATE INDEX idx_location_personne ON location(id_personne);
CREATE INDEX idx_location_etat ON location(etat);

INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role)
VALUES (1, 'CIN001', 'Dupont', 'Jean', '0601020304', 'PERMIS001', 0, 'jean.dupont', 'password123', 'CLIENT');

INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role)
VALUES (2, 'CIN002', 'Martin', 'Sophie', '0607080910', 'PERMIS002', 0, 'sophie.martin', 'password123', 'CLIENT');

INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role)
VALUES (3, 'CIN003', 'Admin', 'Super', '0600000000', 'ADMIN001', 0, 'admin', 'admin123', 'ADMIN');

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix, image_path)
VALUES ('AA-123-BB', 'Renault', 'Clio', 5, 'DISPONIBLE', 0, 50, 'renault_clio.jpg');

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix, image_path)
VALUES ('CC-456-DD', 'Peugeot', '308', 5, 'DISPONIBLE', 0, 65, 'peugeot_308.jpg');

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix, image_path)
VALUES ('EE-789-FF', 'Citroen', 'C3', 5, 'DISPONIBLE', 0, 55, 'citroen_c3.jpg');

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix, image_path)
VALUES ('GG-012-HH', 'Volkswagen', 'Golf', 5, 'LOUEE', 0, 70, 'volkswagen_golf.jpg');

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix, image_path)
VALUES ('II-345-JJ', 'Ford', 'Focus', 5, 'PANNE', 0, 60, 'ford_focus.jpg');

INSERT INTO location (id_location, id_personne, mat, dteDeb, duree, cheque, etat)
VALUES (1, 1, 'GG-012-HH', DATE '2025-05-01', 7, 'CHEQUE001', 'EN_COURS');

CREATE SEQUENCE seq_personne START WITH 4 INCREMENT BY 1;
CREATE SEQUENCE seq_location START WITH 2 INCREMENT BY 1;
