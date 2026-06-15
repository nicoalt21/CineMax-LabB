package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Rappresenta una prenotazione effettuata da un cliente per una proiezione.
 */
public class Prenotazione implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codice;
    private Utente cliente;
    private Proiezione proiezione;
    private int numeroBiglietti;
    private LocalDateTime dataCreazione;

    /**
     * Costruttore vuoto richiesto per la deserializzazione RMI.
     */
    public Prenotazione() {}

    /**
     * Costruttore completo.
     *
     * @param codice codice univoco UUID generato dal server
     * @param cliente utente che ha effettuato la prenotazione
     * @param proiezione proiezione prenotata
     * @param numeroBiglietti numero di biglietti prenotati
     * @param dataCreazione data e ora di creazione della prenotazione
     */
    public Prenotazione(String codice, Utente cliente, Proiezione proiezione, int numeroBiglietti, LocalDateTime dataCreazione) {
        this.codice = codice;
        this.cliente = cliente;
        this.proiezione = proiezione;
        this.numeroBiglietti = numeroBiglietti;
        this.dataCreazione = dataCreazione;
    }

    public String getCodice() { return codice; }
    public void setCodice(String codice) { this.codice = codice; }

    public Utente getCliente() { return cliente; }
    public void setCliente(Utente cliente) { this.cliente = cliente; }

    public Proiezione getProiezione() { return proiezione; }
    public void setProiezione(Proiezione proiezione) { this.proiezione = proiezione; }

    public int getNumeroBiglietti() { return numeroBiglietti; }
    public void setNumeroBiglietti(int numeroBiglietti) { this.numeroBiglietti = numeroBiglietti; }

    public LocalDateTime getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(LocalDateTime dataCreazione) { this.dataCreazione = dataCreazione; }
}