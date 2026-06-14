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

    // costruttori getter e sssetter ;(
}