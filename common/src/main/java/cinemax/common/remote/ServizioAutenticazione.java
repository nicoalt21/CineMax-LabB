package cinemax.common.remote;

import cinemax.common.model.Utente;

import java.rmi.Remote;
import java.rmi.RemoteException;

//interfaccia rmi per i sercizi di autenticazione siummmm

public interface ServizioAutenticazione extends Remote {

    Utente login(String username, String password) throws RemoteException;

    boolean registraCliente(Utente utente) throws RemoteException;

    void logout(String username) throws RemoteException;
}

//login per autenticare, registra per registrare e indovinare logout che fa? :)