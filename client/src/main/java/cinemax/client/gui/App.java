package cinemax.client.gui;

import cinemax.client.gui.navigation.ClientApplication;
import javafx.application.Application;

/**
 * Entry point dell'applicazione client, corrisponde alla mainClass dichiarata nel
 * pom.xml. Si limita a lanciare la {@link ClientApplication} JavaFX.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class App {

    /**
     * Avvia l'applicazione JavaFX.
     *
     * @param args argomenti da riga di comando, inoltrati a JavaFX
     */
    public static void main(String[] args) {
        Application.launch(ClientApplication.class, args);
    }
}
