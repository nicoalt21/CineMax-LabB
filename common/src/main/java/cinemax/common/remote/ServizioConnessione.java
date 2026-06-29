package cinemax.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Servizio RMI dedicato al solo monitoraggio della connessione client-server.
 * <p>
 * Non ha responsabilità di dominio: serve a rendere osservabile lo stato del
 * collegamento. RMI è di per sé stateless e non notifica connessioni o cadute,
 * quindi questo servizio fornisce poche primitive minime:
 * <ul>
 *   <li>{@link #registraConnessione(String)} — il client la chiama una volta,
 *       appena stabilito il collegamento, così il server può stamparlo a
 *       terminale e tenere traccia dei client attivi;</li>
 *   <li>{@link #ping(String)} — il client la chiama periodicamente (heartbeat)
 *       per dimostrare di essere ancora vivo e per verificare, lato suo, che il
 *       server sia ancora raggiungibile;</li>
 *   <li>{@link #chiudiConnessione(String)} — best-effort alla chiusura.</li>
 * </ul>
 * L'{@code idClient} è una stringa opaca generata dal client (vedi
 * {@code StatoConnessione}); identifica la singola sessione nei log del server.
 */
public interface ServizioConnessione extends Remote {

    /**
     * Registra una nuova sessione client presso il server.
     * Il server stampa a terminale l'avvenuta connessione.
     *
     * @param idClient identificativo opaco della sessione client
     * @throws RemoteException in caso di errore RMI
     */
    void registraConnessione(String idClient) throws RemoteException;

    /**
     * Heartbeat periodico. Restituisce sempre {@code true} se la chiamata
     * arriva a destinazione; se il server non è raggiungibile, il client
     * riceve invece una {@link RemoteException} e ne deduce la disconnessione.
     *
     * @param idClient identificativo opaco della sessione client
     * @return {@code true} se il server è vivo e ha ricevuto il ping
     * @throws RemoteException in caso di errore RMI (server irraggiungibile)
     */
    boolean ping(String idClient) throws RemoteException;

    /**
     * Segnala al server che il client sta chiudendo la sessione.
     * Il server stampa a terminale l'avvenuta disconnessione e rimuove il
     * client dall'elenco degli attivi. È un best-effort: se il client muore
     * senza chiamarla, il server se ne accorge per assenza di ping.
     *
     * @param idClient identificativo opaco della sessione client
     * @throws RemoteException in caso di errore RMI
     */
    void chiudiConnessione(String idClient) throws RemoteException;
}
