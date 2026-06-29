package cinemax.server.controller;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger centralizzato per le richieste che arrivano al server.
 * Ogni servizio RMI stampa una riga di richiesta in ingresso (orario, host del client,
 * servizio, operazione e parametri) e una di esito.
 */
public final class LogServer {

    private static final DateTimeFormatter ORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LogServer() {}

    // Riga di richiesta in ingresso. dettaglio: operazione e parametri salienti.
    public static void richiesta(String servizio, String dettaglio) {
        System.out.println("[" + LocalDateTime.now().format(ORA) + "] [" + clientHost() + "] "
                + "[" + servizio + "] -> " + dettaglio);
    }

    // Riga di esito (es. "OK codice=A1B2C3D4" oppure "RIFIUTATA").
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

    // Costruisce una RemoteException sicura da inviare al client. Non allega l'eccezione
    // originale come causa: tipi come PSQLException non sono nel classpath del client e
    // la loro deserializzazione fallirebbe con ClassNotFoundException, mascherando l'errore
    // vero. Qui passa solo testo (messaggio + descrizione della causa).
    public static RemoteException erroreRemoto(String messaggio, Throwable causa) {
        String dettaglioCausa = (causa != null && causa.getMessage() != null)
                ? causa.getMessage()
                : "causa non disponibile";
        return new RemoteException(messaggio + ": " + dettaglioCausa);
    }
}
