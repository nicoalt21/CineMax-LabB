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
 * Implementazione RMI del servizio di monitoraggio connessioni.
 * <p>
 * Mantiene un registro in memoria {@code idClient -> timestamp ultimo contatto}
 * aggiornato tramite heartbeat periodici. Un thread daemon (reaper) rimuove
 * automaticamente i client che non inviano heartbeat entro {@code TIMEOUT_MS}
 * millisecondi, segnalando le cadute a terminale.
 * </p>
 *
 * @author Alt Niccolo' Jacopo, 762605, VA
 * @author Gerti Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ServizioConnessioneImpl extends UnicastRemoteObject implements ServizioConnessione {

    private static final long TIMEOUT_MS = 15_000;
    private static final long INTERVALLO_CONTROLLO_MS = 5_000;
    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, Long> ultimoContatto = new ConcurrentHashMap<>();
    private volatile boolean reaperAttivo = true;
    private Thread reaper;

    /**
     * Costruisce l'implementazione, la esporta come oggetto remoto e avvia il thread reaper.
     *
     * @throws RemoteException in caso di errore durante l'esportazione RMI
     */
    public ServizioConnessioneImpl() throws RemoteException {
        super();
        avviaReaper();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Registra il timestamp corrente per il client e stampa a terminale
     * l'evento di connessione con l'indirizzo IP di provenienza.
     * </p>
     */
    @Override
    public void registraConnessione(String idClient) throws RemoteException {
        ultimoContatto.put(idClient, System.currentTimeMillis());
        log("Nuovo client connesso: " + idClient + provenienza()
                + "  (client attivi: " + ultimoContatto.size() + ")");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Aggiorna il timestamp di ultimo contatto per il client.
     * Se il client non era presente nel registro (es. riconnessione dopo
     * una caduta rilevata dal reaper), lo reiscrive e lo segnala a terminale.
     * </p>
     */
    @Override
    public boolean ping(String idClient) throws RemoteException {
        Long precedente = ultimoContatto.put(idClient, System.currentTimeMillis());
        if (precedente == null) {
            log("Client riconnesso (ping): " + idClient + provenienza()
                    + "  (client attivi: " + ultimoContatto.size() + ")");
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Rimuove il client dal registro e stampa a terminale l'evento di
     * disconnessione. Non ha effetto se il client non era registrato.
     * </p>
     */
    @Override
    public void chiudiConnessione(String idClient) throws RemoteException {
        if (ultimoContatto.remove(idClient) != null) {
            log("Client disconnesso: " + idClient
                    + "  (client attivi: " + ultimoContatto.size() + ")");
        }
    }

    /**
     * Restituisce il numero di client attualmente tracciati nel registro,
     * ovvero quelli che hanno inviato heartbeat entro {@code TIMEOUT_MS}.
     * Metodo di uso interno al server, non esposto via RMI.
     *
     * @return numero di client considerati attivi
     */
    public int numeroClientConnessi() {
        return ultimoContatto.size();
    }

    /**
     * Ferma il thread reaper e lo interrompe se in attesa.
     * Da chiamare durante lo spegnimento pulito del server, prima di uscire dal processo.
     */
    public void fermaReaper() {
        reaperAttivo = false;
        if (reaper != null) {
            reaper.interrupt();
        }
    }

    /**
     * Restituisce l'indirizzo IP del client che ha effettuato la chiamata RMI corrente,
     * formattato come {@code " (host: x.x.x.x)"}. Restituisce una stringa vuota se
     * chiamato al di fuori di un contesto RMI attivo.
     *
     * @return stringa con l'indirizzo IP del client, o stringa vuota
     */
    private String provenienza() {
        try {
            return " (host: " + RemoteServer.getClientHost() + ")";
        } catch (ServerNotActiveException e) {
            return "";
        }
    }

    /**
     * Avvia il thread daemon che controlla periodicamente il registro dei client.
     * Ogni {@code INTERVALLO_CONTROLLO_MS} millisecondi rimuove i client il cui
     * ultimo contatto supera {@code TIMEOUT_MS} millisecondi, segnalando le cadute
     * a terminale. Usa {@link ConcurrentHashMap#remove(Object, Object)} per garantire
     * atomicita' nella rimozione concorrente.
     */
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

    /**
     * Stampa un messaggio a terminale con orario e prefisso {@code [CONNESSIONE]}.
     *
     * @param messaggio testo da stampare
     */
    private void log(String messaggio) {
        System.out.println("[" + LocalTime.now().format(ORA) + "] [CONNESSIONE] " + messaggio);
    }
}