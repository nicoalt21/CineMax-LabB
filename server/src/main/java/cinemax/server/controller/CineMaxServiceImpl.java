/*package cinemax.server.controller;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class CineMaxServiceImpl extends UnicastRemoteObject implements CineMaxService {

    //Il costruttore deve lanciare RemoteExceptio
    public CineMaxServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public List<String> getPalinsesto() throws RemoteException {
        //Dati di prova temporanei
        List<String> film = new ArrayList<>();
        film.add("Interstellar");
        film.add("Inception");
        return film;
    }

    public synchronized boolean prenotaPosto(int idProiezione, int numeroPosto) throws RemoteException {
        //'synchronized serve per la concorrenza richiesta
        System.out.println("Server: Richiesta ricevuta per il posto" + numeroPosto);
        return true;
    }
}*/