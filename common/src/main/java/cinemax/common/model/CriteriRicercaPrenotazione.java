package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDate;

public class CriteriRicercaPrenotazione implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codice;
    private String nomeCliente;
    private String cognomeCliente;
    private String titoloFilm;
    private LocalDate dataInizio;
    private LocalDate dataFine;

    // Costruttore vuoto serve per i filtri opzionali
    public CriteriRicercaPrenotazione(){

    }

    // Costrutore completo
    public CriteriRicercaPrenotazione(String codice, String nomeCliente,String cognomeCliente, String titoloFilm, LocalDate dataInizio, LocalDate dataFine){
        this.codice = codice;
        this.nomeCliente = nomeCliente;
        this.cognomeCliente = cognomeCliente;
        this.titoloFilm = titoloFilm;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    // GETTER e SETTER
    public String getCodice() {
        return codice;
    }
    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }
    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getCognomeCliente() {
        return cognomeCliente;
    }
    public void setCognomeCliente(String cognomeCliente) {
        this.cognomeCliente = cognomeCliente;
    }

    public String getTitoloFilm() {
        return titoloFilm;
    }
    public void setTitoloFilm(String titoloFilm) {
        this.titoloFilm = titoloFilm;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }
    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }
    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }
}