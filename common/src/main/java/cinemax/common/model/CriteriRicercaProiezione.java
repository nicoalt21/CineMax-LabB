package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDate;

public class CriteriRicercaProiezione implements Serializable {

    private static final long serialVersionUID = 1L;

    private String titolo;
    private String genere;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private Double costoMin;
    private Double costoMax;

    // Costruttore vuoto se l'utente non applica filtri subito
    public CriteriRicercaProiezione() {

    }

    // Costruttore completo
    public CriteriRicercaProiezione(String titolo, String genere, LocalDate dataInizio, LocalDate dataFine, Double costoMin, Double costoMax) {
        this.titolo = titolo;
        this.genere = genere;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.costoMin = costoMin;
        this.costoMax = costoMax;
    }

    // GETTER e SETTER
    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getGenere() {
        return genere;
    }

    public void setGenere(String genere) {
        this.genere = genere;
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

    public Double getCostoMin() {
        return costoMin;
    }

    public void setCostoMin(Double costoMin) {
        this.costoMin = costoMin;
    }

    public Double getCostoMax() {
        return costoMax;
    }

    public void setCostoMax(Double costoMax) {
        this.costoMax = costoMax;
    }
}