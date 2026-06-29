package cinemax.server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestisce la connessione al database PostgreSQL.
 * Fornisce il metodo inizializzaConnessione() da chiamare all'avvio del server
 * e getConnection() usato dai DAO per aprire una connessione al database.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class DBconnection {

    private static String url;
    private static String username;
    private static String password;

    /**
     * Inizializza i parametri di connessione al database.
     * Deve essere chiamato una sola volta all'avvio del server in ServerCM,
     * prima di qualsiasi chiamata a getConnection().
     *
     * @param host     indirizzo del server PostgreSQL (es. "localhost")
     * @param porta    porta PostgreSQL (di default 5432)
     * @param database nome del database ("cinemaxdb")
     * @param username username PostgreSQL
     * @param password password PostgreSQL
     */
    public static void inizializzaConnessione(String host, int porta, String database, String username, String password) {
        DBconnection.url      = "jdbc:postgresql://" + host + ":" + porta + "/" + database;
        DBconnection.username = username;
        DBconnection.password = password;
    }

    /**
     * Apre e restituisce una nuova connessione al database.
     * Il chiamante e' responsabile della chiusura.
     *
     * @return connessione aperta al database
     * @throws SQLException          se la connessione non puo' essere stabilita
     * @throws IllegalStateException se inizializzaConnessione() non e' stato ancora chiamato
     */
    public static Connection getConnection() throws SQLException {
        if (url == null) {
            throw new IllegalStateException("DBConnection non inizializzato");
        }
        return DriverManager.getConnection(url, username, password);
    }
}
