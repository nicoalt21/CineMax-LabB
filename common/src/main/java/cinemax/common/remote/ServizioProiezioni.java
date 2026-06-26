package cinemax.common.remote;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Interfaccia RMI per i servizi di gestione delle proiezioni.
 * Le proiezioni sono identificate dalla data_ora (chiave primaria nel DB).
 */
public interface ServizioProiezioni extends Remote {

    /**
     * Cerca proiezioni in base a criteri combinabili.
     *
     * @param criteri criteri di ricerca (titolo, genere, date, costo)
     * @return lista di proiezioni corrispondenti
     * @throws RemoteException in caso di errore RMI
     */
    List<Proiezione> cercaProiezioni(CriteriRicercaProiezione criteri) throws RemoteException;

    /**
     * Restituisce il dettaglio di una proiezione.
     *
     * @param dataOra identificatore della proiezione
     * @return proiezione con posti liberi aggiornati, null se non esiste
     * @throws RemoteException in caso di errore RMI
     */
    Proiezione visualizzaProiezione(LocalDateTime dataOra) throws RemoteException;

    /**
     * Aggiunge una nuova proiezione al palinsesto.
     * Operazione riservata ai proiezionisti.
     * Fallisce se la proiezione si sovrappone a una esistente.
     *
     * @param proiezione proiezione da aggiungere
     * @return true se aggiunta, false se sovrapposizione o vincoli violati
     * @throws RemoteException in caso di errore RMI
     */
    boolean aggiungiProiezione(Proiezione proiezione) throws RemoteException;

    /**
     * Modifica una proiezione esistente (film, data/ora, costo).
     * Operazione riservata ai proiezionisti.
     * La proiezione da modificare e' identificata da dataOraAttuale.
     * Non invalida le prenotazioni esistenti.
     *
     * @param dataOraAttuale identificatore della proiezione da modificare
     * @param idFilm nuovo film
     * @param nuovaDataOra nuova data e ora
     * @param costo nuovo costo biglietto
     * @return true se modificata, false se vincoli violati o proiezione non trovata
     * @throws RemoteException in caso di errore RMI
     */
    boolean modificaProiezione(LocalDateTime dataOraAttuale, int idFilm,
                               LocalDateTime nuovaDataOra, double costo) throws RemoteException;

    /**
     * Elimina una proiezione dal palinsesto.
     * Operazione riservata ai proiezionisti.
     * Fallisce se esistono prenotazioni per quella proiezione.
     *
     * @param dataOra identificatore della proiezione da eliminare
     * @return true se eliminata, false se esistono prenotazioni
     * @throws RemoteException in caso di errore RMI
     */
    boolean eliminaProiezione(LocalDateTime dataOra) throws RemoteException;

    /**
     * Aggiunge un film al catalogo.
     * Fallisce se esiste gia' un film identico su tutti i campi.
     *
     * @param film film da aggiungere
     * @return true se aggiunto, false se duplicato
     * @throws RemoteException in caso di errore RMI
     */
    boolean aggiungiFilm(Film film) throws RemoteException;

    /**
     * Restituisce tutti i film del catalogo.
     *
     * @return lista di tutti i film
     * @throws RemoteException in caso di errore RMI
     */
    List<Film> elencaFilm() throws RemoteException;

    /**
     * Cerca film per titolo parziale (case-insensitive).
     *
     * @param titoloParziale stringa da cercare nel titolo
     * @return lista di film il cui titolo contiene la stringa
     * @throws RemoteException in caso di errore RMI
     */
    List<Film> cercaFilmPerTitolo(String titoloParziale) throws RemoteException;

    /**
     * Restituisce gli orari liberi in cui un film puo' iniziare in un dato giorno.
     *
     * @param giorno giorno di interesse
     * @param durataMinuti durata del film in minuti
     * @param dataOraProiezioneDaEscludere data_ora della proiezione da escludere
     *                                     in fase di modifica, null in fase di creazione
     * @return lista di orari di inizio ammessi (multipli di 5 minuti)
     * @throws RemoteException in caso di errore RMI
     */
    List<LocalTime> finestreLibere(LocalDate giorno, int durataMinuti,
                                   LocalDateTime dataOraProiezioneDaEscludere) throws RemoteException;
}