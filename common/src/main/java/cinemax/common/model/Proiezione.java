package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Proiezione implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idProiezione;
    private Film film;
    private LocalDateTime dataOra;
    private double costoBiglietto;
    private int postiLiberi; // calcolato dal server e non nel DB

    // costruttori
    public Proiezione(int idProiezione, Film film, LocalDateTime dataOra, double costoBiglietto, int postiLiberi) {
        this.idProiezione = idProiezione;
        this.film = film;
        this.dataOra = dataOra;
        this.costoBiglietto = costoBiglietto;
        this.postiLiberi = postiLiberi;
    }

    // GETTER
    public int getIdProiezione() {
        return idProiezione;
    }

    public void setIdProiezione(int idProiezione) {
        this.idProiezione = idProiezione;
    }

    public Film getFilm() {
        return film;
    }

    public void setFilm(Film film) {
        this.film = film;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public void setDataOra(LocalDateTime dataOra) {
        this.dataOra = dataOra;
    }

    public double getCostoBiglietto() {
        return costoBiglietto;
    }

    public void setCostoBiglietto(double costoBiglietto) {
        this.costoBiglietto = costoBiglietto;
    }

    public int getPostiLiberi() {
        return postiLiberi;
    }

    public void setPostiLiberi(int postiLiberi) {
        this.postiLiberi = postiLiberi;
    }
}