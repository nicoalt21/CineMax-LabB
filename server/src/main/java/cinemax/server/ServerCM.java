package cinemax.server;

import cinemax.server.controller.ServizioAutenticazioneImpl;
import cinemax.server.controller.ServizioPrenotazioniImpl;
import cinemax.server.controller.ServizioProiezioniImpl;
import cinemax.server.persistence.DBconnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
        System.out.println("\nConnessione DB configurata.");

        try {
            ServizioAutenticazioneImpl authImpl  = new ServizioAutenticazioneImpl();
            ServizioProiezioniImpl     projImpl  = new ServizioProiezioniImpl();
            ServizioPrenotazioniImpl   prenotImpl = new ServizioPrenotazioniImpl();

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ServizioAutenticazione", authImpl);
            registry.rebind("ServizioProiezioni",     projImpl);
            registry.rebind("ServizioPrenotazioni",   prenotImpl);

            System.out.println("Server RMI attivo sulla porta 1099.");
            System.out.println("In attesa di connessioni...");

        } catch (Exception e) {
            System.err.println("Errore durante l'avvio del server:");
            e.printStackTrace();
        }
    }
}