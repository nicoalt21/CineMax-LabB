package cinemax.common.remote;

import cinemax.common.model.CriteriRicercaPrenotazione;
import cinemax.common.model.Prenotazione;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaccia RMI per i servizi di gestione delle prenotazioni.
 * Le proiezioni sono identificate dalla data_ora.
 */
public interface ServizioPrenotazioni extends Remote {

    /**
     * Crea una nuova prenotazione per una proiezione.
     * Operazione riservata ai clienti registrati.
     * Fallisce se i posti richiesti superano quelli disponibili.
     *
     * @param dataOraProiezione data_ora della proiezione da prenotare
     * @param usernameCliente username del cliente
     * @param numeroBiglietti numero di biglietti da prenotare
     * @return prenotazione creata con codice univoco, null se fallisce
     * @throws RemoteException in caso di errore RMI
     */
    Prenotazione creaPrenotazione(LocalDateTime dataOraProiezione,
                                  String usernameCliente,
                                  int numeroBiglietti) throws RemoteException;

    /**
     * Restituisce le prenotazioni attive di un cliente.
     *
     * @param usernameCliente username del cliente
     * @return lista delle prenotazioni del cliente
     * @throws RemoteException in caso di errore RMI
     */
    List<Prenotazione> visualizzaPrenotazioniCliente(String usernameCliente) throws RemoteException;

    /**
     * Modifica la data di una prenotazione esistente.
     * Entrambe le date (vecchia e nuova proiezione) devono essere future.
     *
     * @param codicePrenotazione codice univoco della prenotazione
     * @param nuovaDataOra nuova data_ora della proiezione
     * @return true se modificata, false se vincoli violati
     * @throws RemoteException in caso di errore RMI
     */
    boolean modificaPrenotazione(String codicePrenotazione,
                                 LocalDateTime nuovaDataOra) throws RemoteException;

    /**
     * Cancella una prenotazione esistente.
     * La data della proiezione deve essere futura.
     *
     * @param codicePrenotazione codice univoco della prenotazione
     * @return true se cancellata, false se proiezione passata o non trovata
     * @throws RemoteException in caso di errore RMI
     */
    boolean cancellaPrenotazione(String codicePrenotazione) throws RemoteException;

    /**
     * Cerca prenotazioni per criteri combinabili.
     * Operazione riservata ai bigliettai.
     *
     * @param criteri criteri di ricerca
     * @return lista di prenotazioni corrispondenti
     * @throws RemoteException in caso di errore RMI
     */
    List<Prenotazione> cercaPrenotazioni(CriteriRicercaPrenotazione criteri) throws RemoteException;

    /**
     * Restituisce le prenotazioni per la data odierna.
     * Operazione riservata ai bigliettai.
     *
     * @return lista delle prenotazioni di oggi
     * @throws RemoteException in caso di errore RMI
     */
    List<Prenotazione> visualizzaPrenotazioniOggi() throws RemoteException;
}