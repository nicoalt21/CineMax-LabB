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

    // 1. Costruttore Completo che serivira anche al DB
    public Prenotazione(String codice, Utente cliente, Proiezione proiezione, int numeroBiglietti, LocalDateTime dataCreazione) {
        this.codice = codice;
        this.cliente = cliente;
        this.proiezione = proiezione;
        this.numeroBiglietti = numeroBiglietti;
        this.dataCreazione = dataCreazione;
    }

    // 2. Costruttore per creare una prenotazione veloce nel Client
    public Prenotazione(String codice, Utente cliente, int numeroBiglietti, Proiezione proiezione, LocalDateTime dataCreazione) {
        this.codice = codice;
        this.cliente = cliente;
        this.numeroBiglietti = numeroBiglietti;
        this.proiezione = proiezione;
        this.dataCreazione = LocalDateTime.now();
    }

    // GETTER
    public String getCodice() {
        return codice;
    }

    public Utente getCliente() {
        return cliente;
    }

    public Proiezione getProiezione() {
        return proiezione;
    }

    public int getNumeroBiglietti() {
        return numeroBiglietti;
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }


}