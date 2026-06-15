package cinemax.common.remote;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Proiezione;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;

public interface ServizioProiezioni extends Remote {

    List<Proiezione> cercaProiezioni(CriteriRicercaProiezione criteri) throws RemoteException;

    Proiezione visualizzaProiezione(int idProiezione) throws RemoteException;

    boolean aggiungiProiezione(Proiezione proiezione) throws RemoteException;

    boolean modificaProiezione(int idProiezione, LocalDateTime nuovaDataOra) throws RemoteException;

    boolean eliminaProiezione(int idProiezione) throws RemoteException;
}

//dovrebbero avere senso ma date un occhio