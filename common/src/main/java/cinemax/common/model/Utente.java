package cinemax.common.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

/**
 * Rappresenta un utente del sistema CineMax.
 */
public class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nome;
    private String cognome;
    private String username;
    private String passwordCifrata;
    private LocalDate dataNascita;
    private String luogoDomicilio;
    private Ruolo ruolo;

    /**
     * Costruttore vuoto richiesto per la deserializzazione RMI.
     */
    public Utente() {}

    /**
     * Costruttore completo.
     *
     * @param nome nome dell'utente
     * @param cognome cognome dell'utente
     * @param username username univoco
     * @param passwordCifrata password cifrata con hash
     * @param dataNascita data di nascita, può essere null
     * @param luogoDomicilio luogo di domicilio
     * @param ruolo ruolo dell'utente nel sistema
     */
    public Utente(String nome, String cognome, String username, String passwordCifrata, LocalDate dataNascita, String luogoDomicilio, Ruolo ruolo) {
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.passwordCifrata = passwordCifrata;
        this.dataNascita = dataNascita;
        this.luogoDomicilio = luogoDomicilio;
        this.ruolo = ruolo;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordCifrata() { return passwordCifrata; }
    public void setPasswordCifrata(String passwordCifrata) { this.passwordCifrata = passwordCifrata; }

    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }

    public String getLuogoDomicilio() { return luogoDomicilio; }
    public void setLuogoDomicilio(String luogoDomicilio) { this.luogoDomicilio = luogoDomicilio; }

    public Ruolo getRuolo() { return ruolo; }
    public void setRuolo(Ruolo ruolo) { this.ruolo = ruolo; }

    public int calcolaEta() {
        if (dataNascita == null) return -1;
        return Period.between(dataNascita, LocalDate.now()).getYears();
    }
}