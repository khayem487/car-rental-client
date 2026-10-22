-- Optional additive schema for an existing Oracle database. Review/run separately.
-- Never use schema-h2.sql or the resetting demo profile against a durable database.
CREATE TABLE oidc_identity (
    provider VARCHAR2(20) NOT NULL,
    subject VARCHAR2(255) NOT NULL,
    personne_id NUMBER NOT NULL UNIQUE,
    email VARCHAR2(320) NOT NULL,
    CONSTRAINT pk_oidc_identity PRIMARY KEY (provider, subject),
    CONSTRAINT fk_oidc_personne FOREIGN KEY (personne_id) REFERENCES personne(id)
);
