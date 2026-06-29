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

    /**
     * Metodo principale. Chiede le credenziali all'utente, si connette a PostgreSQL,
     * crea il database cinemaxdb se non esiste (o lo ricrea se richiesto),
     * poi esegue i file SQL in ordine per creare  e popolare le tabelle.
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

        //connessione al database "postgres" per creare cinemaxdb
        String urlPostgres = "jdbc:postgresql://" + host + ":" + porta + "/postgres";

        System.out.println("\nConnessione a: " + urlPostgres + " in corso...");

        try (Connection conn = DriverManager.getConnection(urlPostgres, username, password)) {

            System.out.println("Connessione riuscita!\n");

            // Controlla se il database esiste già
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
                System.out.println("Il database '" + NOME_DATABASE + "' esiste gia'.");
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
                // Elimina il database esistente
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP DATABASE " + NOME_DATABASE);
                    System.out.println("Database '" + NOME_DATABASE + "' eliminato.");
                }
            }

            // Crea il database
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE " + NOME_DATABASE);
                System.out.println("Database '" + NOME_DATABASE + "' creato correttamente.\n");
            }

        } catch (SQLException e) {
            System.err.println("Errore di connessione");
            System.err.println("Verificare che PostgreSQL sia avviato e le credenziali siano corrette.");
            scanner.close();
            return;
        }

        //Connessione a cinemaxdb ed esecuzione file SQL
        String urlCinemax = "jdbc:postgresql://" + host + ":" + porta + "/" + NOME_DATABASE;

        System.out.println("Connessione a: " + urlCinemax + " in corso...");

        try (Connection conn = DriverManager.getConnection(urlCinemax, username, password)) {

            System.out.println("Connessione riuscita!");
            System.out.println();

            for (String nomeFile : FILE_SQL) {
                eseguiFileSql(conn, nomeFile);
            }

            System.out.println("\n=== Database creato e popolato correttamente! ===");

        } catch (SQLException e) {
            System.err.println("Errore durante l'esecuzione dei file SQL");
        }

        scanner.close();
    }

    /**
     * Legge un file SQL dalle resources e lo esegue istruzione per istruzione.
     * Le istruzioni sono separate dal carattere ";".
     * Le righe vuote e i commenti (--) vengono ignorati.
     *
     * @param conn     connessione al database
     * @param nomeFile nome del file SQL in resources/data/sql/
     */
    private static void eseguiFileSql(Connection conn, String nomeFile) {

        String percorso = "/data/sql/" + nomeFile;

        System.out.print("Esecuzione " + nomeFile + "... ");

        try (InputStream is = DBcreator.class.getResourceAsStream(percorso)) {

            if (is == null) {
                System.out.println("ERRORE: file non trovato: " + percorso);
                return;
            }

            String contenuto = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().reduce("", (a, b) -> a + "\n" + b);

            String[] istruzioni = contenuto.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String istruzione : istruzioni) {
                    String pulita = istruzione.trim();
                    if (!pulita.isEmpty() && !pulita.startsWith("--")) {
                        stmt.execute(pulita);
                    }
                }
                System.out.println("OK");
            }

        } catch (IOException e) {
            System.out.println("ERRORE lettura file");
        } catch (SQLException e) {
            System.out.println("ERRORE SQL");
        }
    }
}