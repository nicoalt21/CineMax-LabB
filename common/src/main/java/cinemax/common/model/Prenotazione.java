package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Prenotazione implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codice; // UUID generato alla creazione
    private Utente cliente;
    private Proiezione proiezione;
    private int numeroBiglietti;
    private LocalDateTime dataCreazione;

    // costruttori ecc
}