package cinemax.server.controller;

import cinemax.common.model.Utente;
import cinemax.common.remote.ServizioAutenticazione;
import cinemax.server.persistence.UtenteDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

public class ServizioAutenticazioneImpl extends UnicastRemoteObject implements ServizioAutenticazione {

    private final UtenteDAO utenteDAO;

    public ServizioAutenticazioneImpl() throws RemoteException {
        super();
        this.utenteDAO = new UtenteDAO();
    }

    @Override
    public Utente login(String username, String passwordHash) throws RemoteException {
        LogServer.richiesta("Autenticazione", "login username=" + username);
        try {
            Utente u = utenteDAO.autenticaUtente(username, passwordHash);
            LogServer.esito("Autenticazione", u != null
                    ? "login OK ruolo=" + u.getRuolo()
                    : "login FALLITO (credenziali errate)");
            return u;
        } catch (SQLException e) {
            LogServer.esito("Autenticazione", "login ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante il login", e);
        }
    }

    @Override
    public boolean registraCliente(Utente utente) throws RemoteException {
        LogServer.richiesta("Autenticazione", "registraCliente username="
                + (utente != null ? utente.getUsername() : "null"));
        try {
            boolean ok = utenteDAO.registraCliente(utente);
            LogServer.esito("Autenticazione", "registraCliente " + (ok ? "OK" : "RIFIUTATA (username gia' in uso?)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Autenticazione", "registraCliente ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la registrazione", e);
        }
    }

    @Override
    public boolean modificaUtente(Utente utente) throws RemoteException {
        LogServer.richiesta("Autenticazione", "modificaUtente username="
                + (utente != null ? utente.getUsername() : "null"));
        try {
            boolean ok = utenteDAO.aggiorna(utente);
            LogServer.esito("Autenticazione", "modificaUtente " + (ok ? "OK" : "RIFIUTATA (utente non trovato)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Autenticazione", "modificaUtente ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la modifica utente", e);
        }
    }

    @Override
    public void logout(String username) throws RemoteException {
        LogServer.richiesta("Autenticazione", "logout username=" + username);
        // RMI è stateless — il logout è gestito lato client
        // Il server non mantiene sessioni attive
    }
}