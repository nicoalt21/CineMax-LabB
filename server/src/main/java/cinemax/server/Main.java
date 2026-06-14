package cinemax.server;

import cinemax.common.CineMaxService;
import cinemax.server.controller.CineMaxServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
        try {
            //1. Creo l'oggetto che sta dentro la cartella controller
            CineMaxService service = new CineMaxServiceImpl();

            // 2. Crea il registro RMI sulla porta standard 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // 3. Registra l'oggetto sulla rete
            registry.rebind("CineMaxService", service);
            System.out.println("=== SERVER CINEMAX RMI ATTIVO SULLA PORTA 1099 ===");
        } catch (Exception e) {
            System.err.println("Errore durante l'avvio del Server RMI");
            e.printStackTrace();
        }
    }
}
