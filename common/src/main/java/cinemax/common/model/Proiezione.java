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

    // costruttori ecc
}