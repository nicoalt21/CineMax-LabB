package cinemax.server.controller;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger centralizzato per le richieste in ingresso al server RMI.
 * <p>
 * Ogni servizio chiama {@link #richiesta} all'inizio dell'operazione e
 * {@link #esito} al termine. Le righe vengono colorate in base all'esito:
 * giallo per le richieste, verde per i successi, rosso per gli errori.
 * I colori possono essere disabilitati impostando la variabile d'ambiente
 * {@code NO_COLOR} a qualsiasi valore.
 * </p>
 *
 * @author Alt Niccolo' Jacopo, 762605, VA
 * @author Gerti Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
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
     * Stampa una riga di richiesta in ingresso, colorata in giallo.
     * Va chiamata all'inizio di ogni metodo RMI, prima di eseguire la logica.
     *
     * @param servizio  nome del servizio RMI (es. {@code "Autenticazione"})
     * @param dettaglio descrizione dell'operazione e dei parametri principali
     */
    public static void richiesta(String servizio, String dettaglio) {
        String riga = "[" + LocalDateTime.now().format(ORA) + "] ["
                + clientHost() + "] [" + servizio + "] -> " + dettaglio;
        System.out.println(colora(GIALLO, riga));
    }

    /**
     * Stampa una riga di esito, colorata in base al contenuto del messaggio.
     * Verde se {@code dettaglio} contiene {@code "OK"}, rosso se contiene
     * {@code "ERRORE"}, {@code "RIFIUTATA"} o {@code "FALLITO"}, bianco altrimenti.
     * Va chiamata al termine di ogni metodo RMI, dopo aver ottenuto il risultato.
     *
     * @param servizio  nome del servizio RMI
     * @param dettaglio descrizione dell'esito dell'operazione
     */
    public static void esito(String servizio, String dettaglio) {
        String riga = "[" + LocalDateTime.now().format(ORA) + "] ["
                + clientHost() + "] [" + servizio + "] <- " + dettaglio;
        System.out.println(colora(scegliColore(dettaglio), riga));
    }

    /**
     * Costruisce una {@link RemoteException} sicura da inviare al client.
     * <p>
     * Non allega l'eccezione originale come causa: tipi come {@code PSQLException}
     * non sono nel classpath del client e la loro deserializzazione fallirebbe con
     * {@link ClassNotFoundException}. Viene trasmesso solo il testo del messaggio.
     * </p>
     *
     * @param messaggio descrizione dell'errore
     * @param causa     eccezione originale da cui estrarre il messaggio; puo' essere null
     * @return {@link RemoteException} contenente solo testo, sicura per la trasmissione RMI
     */
    public static RemoteException erroreRemoto(String messaggio, Throwable causa) {
        String dettaglioCausa = (causa != null && causa.getMessage() != null)
                ? causa.getMessage()
                : "causa non disponibile";
        return new RemoteException(messaggio + ": " + dettaglioCausa);
    }

    /**
     * Determina se i codici colore ANSI devono essere attivati.
     * Restituisce {@code false} se la variabile d'ambiente {@code NO_COLOR}
     * e' impostata, {@code true} in tutti gli altri casi.
     *
     * @return {@code true} se i colori ANSI sono attivi
     */
    private static boolean rilevaSupportoColori() {
        return System.getenv("NO_COLOR") == null;
    }

    /**
     * Applica il codice colore ANSI al testo se i colori sono attivi,
     * altrimenti restituisce il testo invariato.
     *
     * @param codice codice ANSI da applicare (es. {@code VERDE}, {@code ROSSO})
     * @param testo  testo da colorare
     * @return testo colorato o testo invariato se i colori sono disattivi
     */
    private static String colora(String codice, String testo) {
        return COLORI_ATTIVI ? codice + testo + RESET : testo;
    }

    /**
     * Sceglie il codice colore ANSI in base al contenuto del messaggio di esito.
     *
     * @param dettaglio messaggio di esito da analizzare
     * @return codice ANSI per rosso, verde o reset (bianco)
     */
    private static String scegliColore(String dettaglio) {
        if (dettaglio.contains("ERRORE")) return ROSSO;
        if (dettaglio.contains("RIFIUTATA") || dettaglio.contains("FALLITO")) return ROSSO;
        if (dettaglio.contains("OK")) return VERDE;
        return RESET;
    }

    /**
     * Restituisce l'indirizzo IP del client che ha effettuato la chiamata RMI corrente.
     * Se chiamato al di fuori di un contesto RMI attivo, restituisce
     * {@code "host-sconosciuto"}.
     *
     * @return indirizzo IP del client o {@code "host-sconosciuto"}
     */
    private static String clientHost() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "host-sconosciuto";
        }
    }
}