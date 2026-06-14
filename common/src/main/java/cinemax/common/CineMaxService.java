package cinemax.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CineMaxService extends Remote {

    // Restituisce la lista dei film disponibili nel palinsesto
    List<String> getPalinsesto() throws RemoteException;

    // Permette di prenotare un posto per una determinata proiezione
    boolean prenotaPosto(int idProiezione, int numeroPosto) throws RemoteException;
}