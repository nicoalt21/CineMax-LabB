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

    // da fare costruttiru setter e getter
}