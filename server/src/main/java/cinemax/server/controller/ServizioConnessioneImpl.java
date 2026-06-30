package cinemax.server.controller;

import cinemax.common.remote.ServizioConnessione;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servizio di monitoraggio connessioni: tiene un registro in memoria dei client attivi
 * (idClient → ultimo contatto) e stampa a terminale connessioni, disconnessioni e cadute.
 * Un thread reaper rimuove e segnala i client che non inviano heartbeat entro TIMEOUT_MS.
 */
public class ServizioConnessioneImpl extends UnicastRemoteObject implements ServizioConnessione {

    private static final long TIMEOUT_MS = 15_000;
    private static final long INTERVALLO_CONTROLLO_MS = 5_000;
    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, Long> ultimoContatto = new ConcurrentHashMap<>();
    private volatile boolean reaperAttivo = true;
    private Thread reaper;

    public ServizioConnessioneImpl() throws RemoteException {
        super();
        avviaReaper();
    }

    @Override
    public void registraConnessione(String idClient) throws RemoteException {
        ultimoContatto.put(idClient, System.currentTimeMillis());
        log("Nuovo client connesso: " + idClient + provenienza()
                + "  (client attivi: " + ultimoContatto.size() + ")");
    }

    @Override
    public boolean ping(String idClient) throws RemoteException {
        Long precedente = ultimoContatto.put(idClient, System.currentTimeMillis());
        if (precedente == null) {
            log("Client riconnesso (ping): " + idClient + provenienza()
                    + "  (client attivi: " + ultimoContatto.size() + ")");
        }
        return true;
    }

    @Override
    public void chiudiConnessione(String idClient) throws RemoteException {
        if (ultimoContatto.remove(idClient) != null) {
            log("Client disconnesso: " + idClient
                    + "  (client attivi: " + ultimoContatto.size() + ")");
        }
    }

    /**
     * Restituisce il numero di client attualmente tracciati (heartbeat ricevuto entro
     * il timeout). Uso interno del server, non esposto via RMI.
     *
     * @return numero di client connessi
     */
    public int numeroClientConnessi() {
        return ultimoContatto.size();
    }

    /**
     * Ferma il thread reaper. Da chiamare durante lo spegnimento pulito del server.
     */
    public void fermaReaper() {
        reaperAttivo = false;
        if (reaper != null) {
            reaper.interrupt();
        }
    }

    private String provenienza() {
        try {
            return " (host: " + RemoteServer.getClientHost() + ")";
        } catch (ServerNotActiveException e) {
            return "";
        }
    }

    private void avviaReaper() {
        reaper = new Thread(() -> {
            while (reaperAttivo) {
                try {
                    Thread.sleep(INTERVALLO_CONTROLLO_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                long ora = System.currentTimeMillis();
                ultimoContatto.forEach((id, ultimo) -> {
                    if (ora - ultimo > TIMEOUT_MS && ultimoContatto.remove(id, ultimo)) {
                        log("Client caduto (nessun heartbeat): " + id
                                + "  (client attivi: " + ultimoContatto.size() + ")");
                    }
                });
            }
        }, "reaper-connessioni");
        reaper.setDaemon(true);
        reaper.start();
    }

    private void log(String messaggio) {
        System.out.println("[" + LocalTime.now().format(ORA) + "] [CONNESSIONE] " + messaggio);
    }
}