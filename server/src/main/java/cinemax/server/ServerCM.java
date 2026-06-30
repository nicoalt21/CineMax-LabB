package cinemax.server;

import cinemax.server.controller.ServizioAutenticazioneImpl;
import cinemax.server.controller.ServizioConnessioneImpl;
import cinemax.server.controller.ServizioPrenotazioniImpl;
import cinemax.server.controller.ServizioProiezioniImpl;
import cinemax.server.persistence.DBconnection;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Entry point del server CineMax.
 * Chiede le credenziali del DB e la porta RMI, inizializza la connessione,
 * registra i 4 servizi RMI e resta in ascolto di comandi da terminale:
 * 'status' mostra i client connessi, 'stop' arresta il server in modo pulito.
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

        System.out.print("Porta RMI   [1099]: ");
        String portaRmiStr = scanner.nextLine().trim();
        int portaRmi = 1099;
        try {
            if (!portaRmiStr.isEmpty()) portaRmi = Integer.parseInt(portaRmiStr);
        } catch (NumberFormatException e) {
            System.out.println("Porta RMI non valida, uso 1099.");
        }

        DBconnection.inizializzaConnessione(host, porta, database, username, password);

        try (Connection test = DBconnection.getConnection()) {
            System.out.println("\nConnessione DB riuscita.");
        } catch (SQLException e) {
            System.err.println("\nERRORE: impossibile connettersi al database.");
            System.err.println("Causa: " + e.getMessage());
            System.err.println("Controlla che PostgreSQL sia avviato, che il database "
                    + "esista e che le credenziali siano corrette.");
            return;
        }

        Registry registry;
        ServizioConnessioneImpl connImpl;

        try {
            ServizioAutenticazioneImpl authImpl   = new ServizioAutenticazioneImpl();
            ServizioProiezioniImpl     projImpl   = new ServizioProiezioniImpl();
            ServizioPrenotazioniImpl   prenotImpl = new ServizioPrenotazioniImpl();
            connImpl = new ServizioConnessioneImpl();

            registry = LocateRegistry.createRegistry(portaRmi);
            registry.rebind("ServizioAutenticazione", authImpl);
            registry.rebind("ServizioProiezioni",     projImpl);
            registry.rebind("ServizioPrenotazioni",   prenotImpl);
            registry.rebind("ServizioConnessione",    connImpl);

            System.out.println("Server RMI attivo sulla porta " + portaRmi + ".");

        } catch (Exception e) {
            System.err.println("Errore durante l'avvio del server:");
            e.printStackTrace();
            return;
        }

        System.out.println();
        System.out.println("=================================================");
        System.out.println("Server pronto. I log delle richieste appariranno qui sotto.");
        System.out.println("Digita un comando in qualsiasi momento e premi invio:");
        System.out.println("  status  -> mostra il numero di client connessi");
        System.out.println("  stop    -> arresta il server in modo pulito");
        System.out.println("=================================================\n");

        eseguiLoopComandi(scanner, registry, connImpl);
    }

    /**
     * Loop principale: legge comandi da terminale finché non riceve 'stop'.
     * Non stampa un prompt ripetuto a ogni iterazione (si sovrapporrebbe ai log
     * asincroni dei servizi RMI): il comando si digita liberamente in qualsiasi
     * momento, e dopo l'invio viene mostrato un separatore per distinguerlo
     * dai log circostanti.
     *
     * @param scanner  scanner condiviso su System.in
     * @param registry registry RMI da deregistrare alla chiusura
     * @param connImpl servizio connessioni, per il comando 'status'
     */
    private static void eseguiLoopComandi(Scanner scanner, Registry registry,
                                          ServizioConnessioneImpl connImpl) {
        boolean inEsecuzione = true;

        while (inEsecuzione) {
            if (!scanner.hasNextLine()) {
                System.out.println("\nInput terminato, arresto del server.");
                break;
            }

            String comando = scanner.nextLine().trim().toLowerCase();

            if (comando.isEmpty()) {
                continue; // riga vuota, nessun separatore, nessun rumore nei log
            }

            System.out.println("--- comando: " + comando + " ---");

            switch (comando) {
                case "stop":
                    System.out.println("Arresto del server in corso...");
                    inEsecuzione = false;
                    break;
                case "status":
                    System.out.println("Client connessi: " + connImpl.numeroClientConnessi());
                    break;
                default:
                    System.out.println("Comando non riconosciuto. Usa 'status' o 'stop'.");
            }

            System.out.println();
        }

        arrestaServer(registry, connImpl);
    }

    /**
     * Deregistra i servizi RMI e ferma il thread di monitoraggio connessioni.
     *
     * @param registry registry da cui deregistrare i servizi
     * @param connImpl servizio connessioni da fermare
     */
    private static void arrestaServer(Registry registry, ServizioConnessioneImpl connImpl) {
        try {
            registry.unbind("ServizioAutenticazione");
            registry.unbind("ServizioProiezioni");
            registry.unbind("ServizioPrenotazioni");
            registry.unbind("ServizioConnessione");
            connImpl.fermaReaper();
            System.out.println("Server arrestato correttamente.");
        } catch (NotBoundException | RemoteException e) {
            System.err.println("Errore durante l'arresto del server: " + e.getMessage());
        }
    }
}