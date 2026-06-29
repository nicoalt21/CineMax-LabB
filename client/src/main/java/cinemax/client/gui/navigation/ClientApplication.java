package cinemax.client.gui.navigation;

import cinemax.client.service.StatoConnessione;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ClientApplication extends Application {

    @Override
    public void init() {
        caricaFont(
                "/fonts/PlusJakartaSans-Regular.ttf",
                "/fonts/PlusJakartaSans-Light.ttf",
                "/fonts/PlusJakartaSans-SemiBold.ttf",
                "/fonts/PlusJakartaSans-Bold.ttf",
                "/fonts/PlusJakartaSans-ExtraBold.ttf",
                "/fonts/PlusJakartaSans-Italic.ttf",
                "/fonts/DMSerifDisplay-Regular.ttf"
        );
    }

    private void caricaFont(String... percorsi) {
        for (String percorso : percorsi) {
            try (InputStream is = getClass().getResourceAsStream(percorso)) {
                if (is == null) {
                    System.err.println("Font non trovato: " + percorso);
                    continue;
                }
                Font.loadFont(is, 10);
            } catch (IOException e) {
                System.err.println("Errore nel caricamento del font: " + percorso);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Icona della finestra (titlebar + taskbar di Windows).
            URL iconLocation = getClass().getResource("/images/CineMaxIcon.png");
            if (iconLocation != null) {
                primaryStage.getIcons().add(new Image(iconLocation.toExternalForm()));
            } else {
                System.err.println("Attenzione: icona app-icon.png non trovata!");
            }

            primaryStage.setTitle("CineMax - Prenotazione Cinema");
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);

            // Il fornitore dei servizi NON viene più creato qui: all'avvio non
            // sappiamo ancora se il server è raggiungibile, e tentare la
            // connessione adesso farebbe fallire l'intera applicazione quando il
            // server è spento. La connessione reale viene stabilita dalla
            // schermata di connessione (ConnessioneController), che inietta il
            // fornitore nel GestoreScene solo quando il collegamento riesce.
            GestoreScene gestoreScene = new GestoreScene();
            gestoreScene.inizializza(primaryStage, null);

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Errore critico durante l'avvio dell'interfaccia grafica.");
        }
    }

    @Override
    public void stop() {
        // Alla chiusura della finestra: notifica al server la disconnessione e
        // ferma l'heartbeat. Best-effort, non blocca la chiusura.
        StatoConnessione.getInstance().chiudi();
    }
}
