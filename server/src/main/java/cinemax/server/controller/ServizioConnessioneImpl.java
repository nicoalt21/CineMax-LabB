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

    // Oltre questa soglia senza heartbeat il client è considerato caduto.
    private static final long TIMEOUT_MS = 15_000;

    // Ogni quanto il reaper controlla i client scaduti.
    private static final long INTERVALLO_CONTROLLO_MS = 5_000;

    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    // idClient → ultimo istante (epoch ms) in cui si è avuto sue notizie.
    private final Map<String, Long> ultimoContatto = new ConcurrentHashMap<>();

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
        // Se il client ricompare dopo essere stato rimosso per timeout, lo notifichiamo
        // come riconnessione invece di restare silenziosi.
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

    // Host del chiamante RMI tra parentesi, quando disponibile; vuoto altrimenti.
    private String provenienza() {
        try {
            return " (host: " + RemoteServer.getClientHost() + ")";
        } catch (ServerNotActiveException e) {
            return "";
        }
    }

    // Thread demone che rimuove e segnala i client scaduti per assenza di heartbeat.
    private void avviaReaper() {
        Thread reaper = new Thread(() -> {
            while (true) {
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
