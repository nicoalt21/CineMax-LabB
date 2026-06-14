package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nome;
    private String cognome;
    private String username;
    private String passwordCifrata;
    private LocalDate dataNascita; // nullable
    private String luogoDomicilio;
    private Ruolo ruolo;

   // mancano i costruttori ecc.
}