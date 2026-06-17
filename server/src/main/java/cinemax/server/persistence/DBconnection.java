package cinemax.server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection {

    private static String url;
    private static String username;
    private static String password;

    public static void inizializzaConnessione(String host, int porta, String database, String username, String password) {
        DBconnection.url      = "jdbc:postgresql://" + host + ":" + porta + "/" + database;
        DBconnection.username = username;
        DBconnection.password = password;
    }

    public static Connection getConnection() throws SQLException {
        if (url == null) {
            throw new IllegalStateException("DBConnection non inizializzato");
        }
        return DriverManager.getConnection(url, username, password);
    }
}
