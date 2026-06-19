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
        try {
            return utenteDAO.autenticaUtente(username, passwordHash);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il login", e);
        }
    }

    @Override
    public boolean registraCliente(Utente utente) throws RemoteException {
        try {
            return utenteDAO.registraCliente(utente);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la registrazione", e);
        }
    }

    @Override
    public void logout(String username) throws RemoteException {
        // RMI è stateless — il logout è gestito lato clienttt
        // Il server non mantiene sessioni attive
    }
}