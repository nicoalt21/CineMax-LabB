CREATE TYPE ruolo_enum AS ENUM (
    'CLIENTE',
    'PROIEZIONISTA',
    'BIGLIETTAIO'
);

CREATE TABLE utenti(
    username        VARCHAR(50)     PRIMARY KEY,
    nome            VARCHAR(50)     NOT NULL,
    cognome         VARCHAR(50)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    data_nascita    DATE,
    domicilio       VARCHAR(150)    NOT NULL,
    ruolo           ruolo_enum      NOT NULL
);

CREATE TABLE film (
    id_film         SERIAL          PRIMARY KEY,
    titolo          VARCHAR(100)    NOT NULL,
    genere          VARCHAR(100)    NOT NULL,
    regista         VARCHAR(100)    NOT NULL,
    anno            SMALLINT        NOT NULL CHECK (anno BETWEEN 1888 AND 2100),
    durata_minuti   SMALLINT        NOT NULL CHECK (durata_minuti > 0),
    eta_minima      SMALLINT        NOT NULL DEFAULT 0 CHECK (eta_minima >= 0)
);

CREATE TABLE proiezioni (
    data_ora        TIMESTAMP       PRIMARY KEY,
    id_film         INT             NOT NULL REFERENCES film,
    prezzo_biglietto    NUMERIC(6, 2)   NOT NULL CHECK (prezzo_biglietto >= 0)
);

CREATE TABLE prenotazioni (
    codice_univoco      CHAR(8)     PRIMARY KEY,
    username_cliente    VARCHAR(50)     NOT NULL REFERENCES utenti (username),
    data_ora            TIMESTAMP       NOT NULL REFERENCES proiezioni(data_ora),
    numero_posti        SMALLINT        NOT NULL CHECK (numero_posti > 0)
);

