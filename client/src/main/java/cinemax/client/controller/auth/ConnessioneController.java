package cinemax.client.controller.auth;

import cinemax.client.gui.navigation.GestoreScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/*
 Schermata di connessione al server, mostrata all'avvio del client prima dello Start.
 L'utente indica indirizzo IP e porta del server CineMax.

 NOTA: per ora è un placeholder. Il client lavora con i servizi finti in memoria, quindi
 il bottone "Connetti" non apre davvero una connessione: prosegue verso lo Start. Quando
 il server RMI sarà pronto, qui si tenterà la connessione reale
 (FornitoreServizi.creaReale(ip, porta)) e, in caso di errore, si resterà su questa
 schermata mostrando un messaggio, con i campi IP/porta mantenuti.

 Costruita interamente in codice Java (niente FXML), in linea con il resto della UI.
 */
public class ConnessioneController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    private final TextField campoIp = new TextField();
    private final TextField campoPorta = new TextField();
    private final Label labelErrore = new Label();

    public ConnessioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    public VBox getRoot() {
        return radice;
    }

    private VBox costruisciVista() {
        VBox contenitore = new VBox(20);
        contenitore.setAlignment(Pos.CENTER);
        contenitore.getStyleClass().add("sfondo-principale");
        contenitore.setPadding(new Insets(40));

        Label titolo = new Label("Connessione al server");
        titolo.getStyleClass().add("titolo-principale");

        Label sottotitolo = new Label("Indica indirizzo e porta del server CineMax.");
        sottotitolo.getStyleClass().add("testo-secondario");

        // Valori predefiniti comodi per i test in locale.
        campoIp.setText("localhost");
        campoIp.setPromptText("Indirizzo IP");
        campoIp.getStyleClass().add("campo-testo");
        campoIp.setMaxWidth(280);

        campoPorta.setText("1099"); // porta di default del registro RMI
        campoPorta.setPromptText("Porta");
        campoPorta.getStyleClass().add("campo-testo");
        campoPorta.setMaxWidth(280);

        Button btnConnetti = new Button("Connetti");
        btnConnetti.setMaxWidth(280);
        btnConnetti.getStyleClass().add("bottone-primario");
        btnConnetti.setOnAction(e -> onConnettiCliccato());

        labelErrore.getStyleClass().add("campo-errore");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);

        contenitore.getChildren().addAll(titolo, sottotitolo, campoIp, campoPorta, btnConnetti, labelErrore);
        return contenitore;
    }

    private void onConnettiCliccato() {
        pulisciErrore();

        String ip = campoIp.getText();
        String portaTesto = campoPorta.getText();

        if (ip == null || ip.isBlank() || portaTesto == null || portaTesto.isBlank()) {
            mostraErrore("Inserisci indirizzo e porta.");
            return;
        }

        // Validazione minima: la porta deve essere un numero valido.
        try {
            int porta = Integer.parseInt(portaTesto.trim());
            if (porta < 1 || porta > 65535) {
                mostraErrore("La porta deve essere tra 1 e 65535.");
                return;
            }
        } catch (NumberFormatException ex) {
            mostraErrore("La porta deve essere un numero.");
            return;
        }

        // PLACEHOLDER: qui andrà il tentativo di connessione reale al server.
        // Quando il server RMI sarà pronto, qualcosa come:
        //   try {
        //       FornitoreServizi reale = FornitoreServizi.creaReale(ip, porta);
        //       gestoreScene.impostaFornitoreServizi(reale);
        //   } catch (RemoteException | NotBoundException ex) {
        //       mostraErrore("Server non raggiungibile. Riprova.");
        //       return; // resto su questa schermata, campi mantenuti
        //   }
        // Per ora si prosegue sempre, usando i servizi finti già impostati all'avvio.
        gestoreScene.vaiAStart();
    }

    private void mostraErrore(String messaggio) {
        labelErrore.setText(messaggio);
        labelErrore.setManaged(true);
        labelErrore.setVisible(true);
    }

    private void pulisciErrore() {
        labelErrore.setText("");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
    }
}
