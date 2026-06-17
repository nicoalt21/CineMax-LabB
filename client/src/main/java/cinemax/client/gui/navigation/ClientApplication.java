package cinemax.client.gui.navigation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Carichiamo l'fxml della pagina iniziale
            URL fxmlLocation = getClass().getResource("/fxml/auth/start.fxml");
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

    public void Main(String[] args) {
        launch(args);
    } // boh non capisco come farlo partire e non so perchè non vada! :(
    // Lo capirò... !!! :)

}