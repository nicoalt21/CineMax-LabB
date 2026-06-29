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
 * <p>
 * Tiene un identificativo di sessione ({@link #getIdClient()}) generato una
 * volta all'avvio, una property booleana {@code connesso} a cui la UI può
 * legarsi, e un heartbeat periodico verso {@link ServizioConnessione#ping}.
 * <ul>
 *   <li>finché il ping va a buon fine, {@code connesso} resta {@code true};</li>
 *   <li>al primo ping fallito (server irraggiungibile) diventa {@code false};</li>
 *   <li>se il server torna a rispondere, torna {@code true}.</li>
 * </ul>
 * La property viene aggiornata sempre sul thread JavaFX, così l'eventuale label
 * legata può cambiare testo/colore senza problemi di concorrenza.
 * <p>
 * È un singleton di sessione: una sola istanza per esecuzione del client,
 * accessibile via {@link #getInstance()}.
 */
public final class StatoConnessione {

    private static final StatoConnessione ISTANZA = new StatoConnessione();

    /** Intervallo tra un heartbeat e il successivo. */
    private static final long INTERVALLO_PING_SEC = 5;

    /** Identificativo opaco di questa sessione client, mostrato nei log del server. */
    private final String idClient = "client-" + UUID.randomUUID().toString().substring(0, 8);

    private final ReadOnlyBooleanWrapper connesso = new ReadOnlyBooleanWrapper(false);

    /** Servizio remoto usato per gli heartbeat; null finché non si è connessi. */
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

    /** Property osservabile: {@code true} se il client è collegato al server. */
    public ReadOnlyBooleanProperty connessoProperty() {
        return connesso.getReadOnlyProperty();
    }

    public boolean isConnesso() {
        return connesso.get();
    }

    /**
     * Avvia il monitoraggio: registra il client presso il server (che lo stampa
     * a terminale) e fa partire l'heartbeat periodico. Da chiamare una volta,
     * appena ottenuto lo stub {@link ServizioConnessione} reale.
     *
     * @param servizio stub remoto del servizio di connessione
     * @throws RemoteException se la registrazione iniziale fallisce
     */
    public void avviaMonitoraggio(ServizioConnessione servizio) throws RemoteException {
        this.servizioConnessione = servizio;
        servizio.registraConnessione(idClient); // il server stampa la connessione
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

    /** Un singolo battito dell'heartbeat: aggiorna lo stato in base all'esito. */
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

    /** Aggiorna la property sul thread JavaFX, evitando notifiche ridondanti. */
    private void impostaConnesso(boolean valore) {
        Platform.runLater(() -> {
            if (connesso.get() != valore) {
                connesso.set(valore);
            }
        });
    }

    /**
     * Comunica al server la chiusura della sessione (best-effort) e ferma
     * l'heartbeat. Da chiamare alla chiusura dell'applicazione.
     */
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
