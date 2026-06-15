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

    // 1. Costruttore completo: Utile per quando si leggersano i dati del database
    public Utente(String nome, String cognome, String username, String passwordCifrata, LocalDate dataNascita, String luogoDomicilio, Ruolo ruolo) {
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.passwordCifrata = passwordCifrata;
        this.dataNascita = dataNascita;
        this.luogoDomicilio = luogoDomicilio;
        this.ruolo = ruolo;
    }

    // 2. Costuttore leggero: Comando per creare un utente al volo nei test di login
    public Utente(String nome, String cognome) {
        this.nome = nome;
        this.cognome = cognome;
    }

    // 3. GETTER: Permettono alle altre parti del programma (e al client) di leggere i dati
    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordCifrata() {
        return passwordCifrata;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public String getLuogoDomicilio() {
        return luogoDomicilio;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }
}