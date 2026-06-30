package cinemax.common.remote;

import cinemax.common.model.Utente;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServizioAutenticazione extends Remote {

    Utente login(String username, String passwordHash) throws RemoteException;

    boolean registraCliente(Utente utente) throws RemoteException;

    boolean modificaUtente(Utente utente) throws RemoteException;

    void logout(String username) throws RemoteException;
}