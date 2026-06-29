package cinemax.server.controller;

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger semplice e centralizzato per le richieste che arrivano al server.
 * <p>
 * Ogni servizio RMI, all'inizio di ogni metodo, chiama {@link #richiesta(String, String)}
 * per stampare a terminale una riga con: orario, host del client chiamante, servizio e
 * operazione richiesta, più gli eventuali parametri salienti. In coda si può registrare
 * l'esito con {@link #esito(String, String)}.
 * <p>
 * L'host del client viene ricavato da {@link RemoteServer#getClientHost()}, valido solo
 * mentre è in corso una chiamata RMI; se non disponibile si usa un segnaposto.
 */
public final class LogServer {

    private static final DateTimeFormatter ORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LogServer() {}

    /**
     * Stampa una riga di richiesta in ingresso.
     *
     * @param servizio   nome del servizio (es. "Prenotazioni")
     * @param dettaglio  operazione e parametri (es. "creaPrenotazione cliente=mario posti=2")
     */
    public static void richiesta(String servizio, String dettaglio) {
        System.out.println("[" + LocalDateTime.now().format(ORA) + "] [" + clientHost() + "] "
                + "[" + servizio + "] -> " + dettaglio);
    }

    /**
     * Stampa l'esito di una richiesta (riga di completamento), indentata sotto la richiesta.
     *
     * @param servizio nome del servizio
     * @param dettaglio descrizione dell'esito (es. "OK codice=A1B2C3D4" oppure "RIFIUTATA")
     */
    public static void esito(String servizio, String dettaglio) {
        System.out.println("[" + LocalDateTime.now().format(ORA) + "] [" + clientHost() + "] "
                + "[" + servizio + "] <- " + dettaglio);
    }

    private static String clientHost() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "host-sconosciuto";
        }
    }
}
