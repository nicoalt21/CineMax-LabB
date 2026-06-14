package cinemax.common.model;

import java.io.Serializable;

public class Film implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idFilm;
    private String titolo;
    private String genere;
    private String regista;
    private int anno;
    private int durataMinuti;
    private int etaMinima;
}