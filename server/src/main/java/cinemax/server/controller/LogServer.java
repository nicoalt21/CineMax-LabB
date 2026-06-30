package cinemax.server.controller;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger centralizzato per le richieste che arrivano al server.
 * Ogni servizio RMI stampa una riga di richiesta in ingresso (orario, host del client,
 * servizio, operazione e parametri) e una di esito, colorate per leggibilità rapida.
 */
public final class LogServer {

    private static final DateTimeFormatter ORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String VERDE  = "\u001B[32m";
    private static final String ROSSO  = "\u001B[31m";
    private static final String GIALLO = "\u001B[33m";
    private static final String RESET  = "\u001B[0m";

    private static final boolean COLORI_ATTIVI = rilevaSupportoColori();

    private LogServer() {}

    /**
     * Decide se attivare i colori ANSI. Disattivabile esplicitamente con la
     * variabile d'ambiente NO_COLOR (convenzione standard). Attivo di default
     * su Linux/Mac e su Windows (inclusi i terminali moderni e quello di IntelliJ).
     */
    private static boolean rilevaSupportoColori() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        return true;
    }

    private static String colora(String codice, String testo) {
        return COLORI_ATTIVI ? codice + testo + RESET : testo;
    }

    public static void richiesta(String servizio, String dettaglio) {
        String riga = "[" + LocalDateTime.now().format(ORA) + "] ["
                + clientHost() + "] [" + servizio + "] -> " + dettaglio;
        System.out.println(colora(GIALLO, riga));
    }

    public static void esito(String servizio, String dettaglio) {
        String riga = "[" + LocalDateTime.now().format(ORA) + "] ["
                + clientHost() + "] [" + servizio + "] <- " + dettaglio;
        System.out.println(colora(scegliColore(dettaglio), riga));
    }

    private static String scegliColore(String dettaglio) {
        if (dettaglio.contains("ERRORE")) return ROSSO;
        if (dettaglio.contains("RIFIUTATA") || dettaglio.contains("FALLITO")) return ROSSO;
        if (dettaglio.contains("OK")) return VERDE;
        return RESET;
    }

    private static String clientHost() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "host-sconosciuto";
        }
    }

    /**
     * Costruisce una RemoteException sicura da inviare al client. Non allega l'eccezione
     * originale come causa: tipi come PSQLException non sono nel classpath del client e
     * la loro deserializzazione fallirebbe con ClassNotFoundException, mascherando l'errore
     * vero. Qui passa solo testo (messaggio + descrizione della causa).
     */
    public static RemoteException erroreRemoto(String messaggio, Throwable causa) {
        String dettaglioCausa = (causa != null && causa.getMessage() != null)
                ? causa.getMessage()
                : "causa non disponibile";
        return new RemoteException(messaggio + ": " + dettaglioCausa);
    }
}