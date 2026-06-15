package cinemax.common.remote;

import cinemax.common.model.CriteriRicercaPrenotazione;
import cinemax.common.model.Prenotazione;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;

public interface ServizioPrenotazioni extends Remote {

    Prenotazione creaPrenotazione(int idProiezione, String usernameCliente, int numeroBiglietti) throws RemoteException;

    List<Prenotazione> visualizzaPrenotazioniCliente(String usernameCliente) throws RemoteException;

    boolean modificaPrenotazione(String codicePrenotazione, LocalDateTime nuovaDataOra) throws RemoteException;

    boolean cancellaPrenotazione(String codicePrenotazione) throws RemoteException;

    List<Prenotazione> cercaPrenotazioni(CriteriRicercaPrenotazione criteri) throws RemoteException;

    List<Prenotazione> visualizzaPrenotazioniOggi() throws RemoteException;
}

//dovrebbero avere senso ma date un occhio