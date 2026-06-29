package cinemax.client.service;

import cinemax.common.remote.ServizioConnessione;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.rmi.RemoteException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Stato condiviso della connessione al server, osservabile dalla UI.
 * Singleton di sessione con un id client generato all'avvio, una property {@code connesso}
 * a cui la UI si lega, e un heartbeat periodico verso {@link ServizioConnessione#ping}:
 * finché il ping riesce {@code connesso} è true, al primo fallimento diventa false, e torna
 * true se il server ridiventa raggiungibile. La property è sempre aggiornata sul thread JavaFX.
 */
public final class StatoConnessione {

    private static final StatoConnessione ISTANZA = new StatoConnessione();

    private static final long INTERVALLO_PING_SEC = 5;

    // Id opaco di questa sessione, mostrato nei log del server.
    private final String idClient = "client-" + UUID.randomUUID().toString().substring(0, 8);

    private final ReadOnlyBooleanWrapper connesso = new ReadOnlyBooleanWrapper(false);

    // Stub remoto per gli heartbeat; null finché non si è connessi.
    private volatile ServizioConnessione servizioConnessione;

    private ScheduledExecutorService scheduler;

    private StatoConnessione() {
    }

    public static StatoConnessione getInstance() {
        return ISTANZA;
    }

    public String getIdClient() {
        return idClient;
    }

    // Property osservabile: true se il client è collegato al server.
    public ReadOnlyBooleanProperty connessoProperty() {
        return connesso.getReadOnlyProperty();
    }

    public boolean isConnesso() {
        return connesso.get();
    }

    // Registra il client presso il server (che lo stampa) e avvia l'heartbeat periodico.
    // Da chiamare una volta, appena ottenuto lo stub reale.
    public void avviaMonitoraggio(ServizioConnessione servizio) throws RemoteException {
        this.servizioConnessione = servizio;
        servizio.registraConnessione(idClient);
        impostaConnesso(true);

        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "heartbeat-connessione");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(this::battito,
                    INTERVALLO_PING_SEC, INTERVALLO_PING_SEC, TimeUnit.SECONDS);
        }
    }

    // Un singolo battito: aggiorna lo stato in base all'esito del ping.
    private void battito() {
        ServizioConnessione s = servizioConnessione;
        if (s == null) {
            impostaConnesso(false);
            return;
        }
        try {
            s.ping(idClient);
            impostaConnesso(true);
        } catch (RemoteException e) {
            impostaConnesso(false);
        }
    }

    // Aggiorna la property sul thread JavaFX, evitando notifiche ridondanti.
    private void impostaConnesso(boolean valore) {
        Platform.runLater(() -> {
            if (connesso.get() != valore) {
                connesso.set(valore);
            }
        });
    }

    // Comunica al server la chiusura (best-effort) e ferma l'heartbeat.
    public void chiudi() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        ServizioConnessione s = servizioConnessione;
        if (s != null) {
            try {
                s.chiudiConnessione(idClient);
            } catch (RemoteException ignored) {
                // chiusura best-effort: se il server è già irraggiungibile, pazienza
            }
        }
        servizioConnessione = null;
        impostaConnesso(false);
    }
}
