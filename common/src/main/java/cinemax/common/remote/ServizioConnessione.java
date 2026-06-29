package cinemax.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Servizio RMI per il solo monitoraggio della connessione client-server.
 * RMI non notifica connessioni né cadute, quindi il client si registra una volta
 * ({@link #registraConnessione}), invia heartbeat periodici ({@link #ping}) e segnala la
 * chiusura ({@link #chiudiConnessione}). L'idClient è una stringa opaca generata dal
 * client che identifica la sessione nei log del server.
 */
public interface ServizioConnessione extends Remote {

    // Registra una nuova sessione client (il server la stampa a terminale).
    void registraConnessione(String idClient) throws RemoteException;

    // Heartbeat periodico: torna true se il server riceve il ping; in caso di server
    // irraggiungibile il client riceve una RemoteException e deduce la disconnessione.
    boolean ping(String idClient) throws RemoteException;

    // Chiusura best-effort della sessione. Se il client muore senza chiamarla, il server
    // se ne accorge per assenza di ping.
    void chiudiConnessione(String idClient) throws RemoteException;
}
