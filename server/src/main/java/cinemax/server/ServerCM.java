package cinemax.server;

import cinemax.server.controller.ServizioAutenticazioneImpl;
import cinemax.server.controller.ServizioConnessioneImpl;
import cinemax.server.controller.ServizioPrenotazioniImpl;
import cinemax.server.controller.ServizioProiezioniImpl;
import cinemax.server.persistence.DBconnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Entry point del server CineMax.
 * Chiede le credenziali del DB, inizializza la connessione,
 * registra i 3 servizi RMI e resta in ascolto.
 */
public class ServerCM {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CineMax Server ===\n");

        System.out.print("Host DB     [localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Porta DB    [5432]: ");
        String portaStr = scanner.nextLine().trim();
        int porta = 5432;
        try {
            if (!portaStr.isEmpty()) porta = Integer.parseInt(portaStr);
        } catch (NumberFormatException e) {
            System.out.println("Porta non valida, uso 5432.");
        }

        System.out.print("Database    [cinemaxdb]: ");
        String database = scanner.nextLine().trim();
        if (database.isEmpty()) database = "cinemaxdb";

        System.out.print("Username DB [postgres]: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) username = "postgres";

        System.out.print("Password DB: ");
        String password = scanner.nextLine().trim();

        DBconnection.inizializzaConnessione(host, porta, database, username, password);

        // Test reale della connessione: senza questo, un DB irraggiungibile o
        // credenziali errate non darebbero errore qui, ma solo alla prima
        // operazione del client. Meglio fallire subito e in chiaro.
        try (Connection test = DBconnection.getConnection()) {
            System.out.println("\nConnessione DB riuscita.");
        } catch (SQLException e) {
            System.err.println("\nERRORE: impossibile connettersi al database.");
            System.err.println("Causa: " + e.getMessage());
            System.err.println("Controlla che PostgreSQL sia avviato, che il database "
                    + "esista e che le credenziali siano corrette.");
            return; // non avviare il server RMI con un DB irraggiungibile
        }

        try {
            ServizioAutenticazioneImpl authImpl   = new ServizioAutenticazioneImpl();
            ServizioProiezioniImpl     projImpl   = new ServizioProiezioniImpl();
            ServizioPrenotazioniImpl   prenotImpl = new ServizioPrenotazioniImpl();
            ServizioConnessioneImpl    connImpl   = new ServizioConnessioneImpl();

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ServizioAutenticazione", authImpl);
            registry.rebind("ServizioProiezioni",     projImpl);
            registry.rebind("ServizioPrenotazioni",   prenotImpl);
            registry.rebind("ServizioConnessione",    connImpl);

            System.out.println("Server RMI attivo sulla porta 1099.");
            System.out.println("In attesa di connessioni... (Ctrl+C per terminare)");

            // Tiene vivo il processo in modo esplicito, senza affidarsi al fatto
            // che i thread RMI siano non-daemon. Il server resta in ascolto finche'
            // non viene interrotto manualmente.
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }

        } catch (InterruptedException e) {
            System.out.println("Server interrotto, chiusura in corso.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Errore durante l'avvio del server:");
            e.printStackTrace();
        }
    }
}