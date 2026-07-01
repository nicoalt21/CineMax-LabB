package cinemax.dbcreator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Classe per la creazione e il popolamento del database CineMax.
 * Chiede le credenziali PostgreSQL all'utente, crea il database cinemaxdb
 * ed esegue in ordine i file SQL per creare le tabelle e popolarle con i dati iniziali.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class DBcreator {

    private static final String NOME_DATABASE = "cinemaxdb";

    private static final String[] FILE_SQL = {
            "schema.sql",
            "utenti.sql",
            "film.sql",
            "proiezioni.sql",
            "prenotazioni.sql"
    };

    private static final String VERDE  = "\u001B[32m";
    private static final String ROSSO  = "\u001B[31m";
    private static final String GIALLO = "\u001B[33m";
    private static final String RESET  = "\u001B[0m";

    private static final boolean COLORI_ATTIVI = rilevaSupportoColori();

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

    private static void stampaOk(String testo) {
        System.out.println(colora(VERDE, testo));
    }

    private static void stampaErrore(String testo) {
        System.out.println(colora(ROSSO, testo));
    }

    private static void stampaInfo(String testo) {
        System.out.println(colora(GIALLO, testo));
    }

    /**
     * Metodo principale. Chiede le credenziali all'utente, si connette a PostgreSQL,
     * crea il database cinemaxdb se non esiste (o lo ricrea se richiesto),
     * poi esegue i file SQL in ordine per creare le tabelle e inserire i dati.
     *
     * @param args argomenti della riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CineMax - Creazione Database ===\n");

        System.out.print("Host     [localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Porta    [5432]: ");
        String portaStr = scanner.nextLine().trim();
        int porta;
        try {
            porta = portaStr.isEmpty() ? 5432 : Integer.parseInt(portaStr);
        } catch (NumberFormatException e) {
            System.out.println("Errore: la porta deve essere un numero. Valore di default 5432 utilizzato.");
            porta = 5432;
        }

        System.out.print("Username [postgres]: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) username = "postgres";

        System.out.print("Password (premi invio se vuota): ");
        String password = scanner.nextLine().trim();

        String urlPostgres = "jdbc:postgresql://" + host + ":" + porta + "/postgres";

        stampaInfo("\nConnessione a: " + urlPostgres + " in corso...");

        try (Connection conn = DriverManager.getConnection(urlPostgres, username, password)) {

            stampaOk("Connessione riuscita!\n");

            boolean esiste = false;
            try (var rs = conn.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    if (NOME_DATABASE.equals(rs.getString(1))) {
                        esiste = true;
                        break;
                    }
                }
            }

            if (esiste) {
                stampaInfo("Il database '" + NOME_DATABASE + "' esiste gia'.");
                String risposta;
                while (true) {
                    System.out.print("Vuoi eliminarlo e ricrearlo? (s/n): ");
                    risposta = scanner.nextLine().trim().toLowerCase();
                    if (risposta.equals("s") || risposta.equals("n")) {
                        break;
                    }
                    System.out.println("Input non valido. Inserire 's' o 'n'.");
                }

                if (!risposta.equals("s")) {
                    System.out.println("Operazione annullata.");
                    scanner.close();
                    return;
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP DATABASE " + NOME_DATABASE);
                    stampaInfo("Database '" + NOME_DATABASE + "' eliminato.");
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE " + NOME_DATABASE);
                stampaOk("Database '" + NOME_DATABASE + "' creato correttamente.\n");
            }

        } catch (SQLException e) {
            stampaErrore("Errore di connessione: " + e.getMessage());
            stampaErrore("Verificare che PostgreSQL sia avviato e le credenziali siano corrette.");
            scanner.close();
            return;
        }

        String urlCinemax = "jdbc:postgresql://" + host + ":" + porta + "/" + NOME_DATABASE;

        stampaInfo("Connessione a: " + urlCinemax + " in corso...");

        boolean tuttoOk = true;

        try (Connection conn = DriverManager.getConnection(urlCinemax, username, password)) {

            stampaOk("Connessione riuscita!\n");

            for (String nomeFile : FILE_SQL) {
                boolean ok = eseguiFileSql(conn, nomeFile);
                if (!ok) tuttoOk = false;
            }

        } catch (SQLException e) {
            stampaErrore("Errore durante la connessione per l'esecuzione dei file SQL: " + e.getMessage());
            tuttoOk = false;
        }

        System.out.println();
        if (tuttoOk) {
            stampaOk("=== Database creato e popolato correttamente! ===");
        } else {
            stampaErrore("=== Database creato, ma con uno o piu' errori durante il popolamento. ===");
            stampaErrore("Controllare i messaggi sopra per individuare il file SQL che ha fallito.");
        }

        scanner.close();
    }


    /**
     * Esegue tutte le istruzioni SQL contenute in un file delle risorse.
     * In caso di errore su una singola istruzione, lo segnala con il numero
     * dell'istruzione e continua con le successive, così un solo errore
     * non blocca l'intero file.
     *
     * @param conn     connessione al database
     * @param nomeFile nome del file SQL nelle risorse (es. "schema.sql")
     * @return true se tutte le istruzioni sono andate a buon fine, false altrimenti
     */
    private static boolean eseguiFileSql(Connection conn, String nomeFile) {

        String percorso = "/data/sql/" + nomeFile;

        System.out.print("Esecuzione " + nomeFile + "... ");

        try (InputStream is = DBcreator.class.getResourceAsStream(percorso)) {

            if (is == null) {
                stampaErrore("ERRORE: file non trovato: " + percorso);
                return false;
            }

            String contenuto = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().reduce("", (a, b) -> a + "\n" + b);

            String[] istruzioni = contenuto.split(";");
            boolean ok = true;
            int numeroIstruzione = 0;

            try (Statement stmt = conn.createStatement()) {
                for (String istruzione : istruzioni) {
                    String pulita = istruzione.trim();
                    if (pulita.isEmpty() || pulita.startsWith("--")) {
                        continue;
                    }
                    numeroIstruzione++;
                    try {
                        stmt.execute(pulita);
                    } catch (SQLException e) {
                        System.out.println();
                        stampaErrore("  ERRORE nell'istruzione #" + numeroIstruzione
                                + " di " + nomeFile + ": " + e.getMessage());
                        ok = false;
                    }
                }
            }

            if (ok) {
                stampaOk("OK (" + numeroIstruzione + " istruzioni)");
            }
            return ok;

        } catch (IOException e) {
            stampaErrore("ERRORE lettura file: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            stampaErrore("ERRORE SQL: " + e.getMessage());
            return false;
        }
    }
}