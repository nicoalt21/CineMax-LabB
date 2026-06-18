package cinemax.client.gui.navigation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ClientApplication extends Application {

    /**
     * Carica i font custom PRIMA che venga costruita la UI.
     * Una volta registrati qui, sono richiamabili dal CSS tramite
     * -fx-font-family con il loro nome reale (es. "Plus Jakarta Sans").
     */
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
            // Carichiamo l'fxml della pagina iniziale
            URL fxmlLocation = getClass().getResource("/fxml/start.fxml");
            if (fxmlLocation == null) {
                throw new IllegalStateException("Impossibile trovare il file start.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene scene = new Scene(root, 800, 600);

            URL cssLocation = getClass().getResource("/css/theme.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
            } else {
                System.err.println("Attenzione: theme.css non trovato!");
            }

            // Icona della finestra (titlebar + taskbar di Windows).
            URL iconLocation = getClass().getResource("/images/CineMaxIcon.png");
            if (iconLocation != null) {
                primaryStage.getIcons().add(new Image(iconLocation.toExternalForm()));
            } else {
                System.err.println("Attenzione: icona app-icon.png non trovata!");
            }

            primaryStage.setTitle("CineMax - Prenotazione Cinema");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore critico durante l'avvio dell'interfaccia grafica.");
        }
    }
}
