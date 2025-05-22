-- Suppression des tables existantes (si elles existent)
DROP TABLE location CASCADE CONSTRAINTS;
DROP TABLE voiture CASCADE CONSTRAINTS;
DROP TABLE personne CASCADE CONSTRAINTS;

-- Suppression des séquences existantes (si elles existent)
DROP SEQUENCE seq_personne;
DROP SEQUENCE seq_location;

-- Création de la table personne
CREATE TABLE personne (
    id NUMBER PRIMARY KEY,
    cin VARCHAR2(20) UNIQUE NOT NULL,
    nom VARCHAR2(50) NOT NULL,
    prenom VARCHAR2(50) NOT NULL,
    numero VARCHAR2(20) NOT NULL,
    npermis VARCHAR2(20) NOT NULL,
    nbloc NUMBER DEFAULT 0,
    login VARCHAR2(50) UNIQUE NOT NULL,
    passwd VARCHAR2(100) NOT NULL,
    role VARCHAR2(20) NOT NULL
);

-- Création de la table voiture
CREATE TABLE voiture (
    mat VARCHAR2(20) PRIMARY KEY,
    marque VARCHAR2(50) NOT NULL,
    model VARCHAR2(50) NOT NULL,
    nbplace NUMBER NOT NULL,
    disp VARCHAR2(20) NOT NULL, -- DISPONIBLE, LOUEE, PANNE
    nbloc NUMBER DEFAULT 0,
    prix NUMBER NOT NULL
);

-- Création de la table location
CREATE TABLE location (
    id_location NUMBER PRIMARY KEY,
    id_personne NUMBER NOT NULL,
    mat VARCHAR2(20) NOT NULL,
    dteDeb DATE NOT NULL,
    duree NUMBER NOT NULL,
    cheque VARCHAR2(50) NOT NULL,
    etat VARCHAR2(20) NOT NULL, -- EN_COURS, TERMINEE
    FOREIGN KEY (id_personne) REFERENCES personne(id),
    FOREIGN KEY (mat) REFERENCES voiture(mat)
);

-- Création des séquences
CREATE SEQUENCE seq_personne
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE seq_location
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Création des index
CREATE INDEX idx_personne_login ON personne(login);
CREATE INDEX idx_voiture_disp ON voiture(disp);
CREATE INDEX idx_location_personne ON location(id_personne);
CREATE INDEX idx_location_etat ON location(etat);

-- Insertion de données d'exemple pour les personnes (clients et administrateurs)
INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role)
VALUES (seq_personne.NEXTVAL, 'CIN001', 'Dupont', 'Jean', '0601020304', 'PERMIS001', 0, 'jean.dupont', 'password123', 'CLIENT');

INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role)
VALUES (seq_personne.NEXTVAL, 'CIN002', 'Martin', 'Sophie', '0607080910', 'PERMIS002', 0, 'sophie.martin', 'password123', 'CLIENT');

INSERT INTO personne (id, cin, nom, prenom, numero, npermis, nbloc, login, passwd, role)
VALUES (seq_personne.NEXTVAL, 'CIN003', 'Admin', 'Super', '0600000000', 'ADMIN001', 0, 'admin', 'admin123', 'ADMIN');

-- Insertion de données d'exemple pour les voitures
INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix)
VALUES ('AA-123-BB', 'Renault', 'Clio', 5, 'DISPONIBLE', 0, 50);

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix)
VALUES ('CC-456-DD', 'Peugeot', '308', 5, 'DISPONIBLE', 0, 65);

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix)
VALUES ('EE-789-FF', 'Citroen', 'C3', 5, 'DISPONIBLE', 0, 55);

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix)
VALUES ('GG-012-HH', 'Volkswagen', 'Golf', 5, 'LOUEE', 0, 70);

INSERT INTO voiture (mat, marque, model, nbplace, disp, nbloc, prix)
VALUES ('II-345-JJ', 'Ford', 'Focus', 5, 'PANNE', 0, 60);

-- Insertion de données d'exemple pour les locations
INSERT INTO location (id_location, id_personne, mat, dteDeb, duree, cheque, etat)
VALUES (seq_location.NEXTVAL, 1, 'GG-012-HH', TO_DATE('2025-05-01', 'YYYY-MM-DD'), 7, 'CHEQUE001', 'EN_COURS');

COMMIT; 