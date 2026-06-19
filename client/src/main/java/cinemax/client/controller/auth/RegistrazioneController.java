package cinemax.client.controller.auth;

import cinemax.client.gui.navigation.GestoreScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// Schermata di registrazione, costruita interamente in codice Java (come StartController). Riceve il GestoreScene nel costruttore per poter navigare.
public class RegistrazioneController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    private final TextField campoNome = new TextField();
    private final TextField campoUsername = new TextField();
    private final PasswordField campoPassword = new PasswordField();
    private final PasswordField campoConfermaPassword = new PasswordField();

    public RegistrazioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    // Restituisce il nodo radice della schermata, da inserire nella Scene.
    public VBox getRoot() {
        return radice;
    }

    private VBox costruisciVista() {
        VBox contenitore = new VBox(20);
        contenitore.setAlignment(Pos.CENTER);
        contenitore.getStyleClass().add("sfondo-principale");
        contenitore.setPadding(new Insets(40));

        Label titolo = new Label("Registrati");
        titolo.getStyleClass().add("titolo-principale");

        campoNome.setPromptText("Nome");
        campoNome.getStyleClass().add("campo-testo");
        campoNome.setMaxWidth(280);

        campoUsername.setPromptText("Username");
        campoUsername.getStyleClass().add("campo-testo");
        campoUsername.setMaxWidth(280);

        campoPassword.setPromptText("Password");
        campoPassword.getStyleClass().add("campo-testo");
        campoPassword.setMaxWidth(280);

        campoConfermaPassword.setPromptText("Conferma password");
        campoConfermaPassword.getStyleClass().add("campo-testo");
        campoConfermaPassword.setMaxWidth(280);

        Button btnConferma = new Button("Conferma");
        btnConferma.setMaxWidth(280);
        btnConferma.getStyleClass().add("bottone-primario");
        btnConferma.setOnAction(e -> onConfermaRegistrazioneCliccato());

        Button btnAnnulla = new Button("Annulla");
        btnAnnulla.setMaxWidth(280);
        btnAnnulla.getStyleClass().add("bottone-secondario");
        btnAnnulla.setOnAction(e -> onAnnullaCliccato());

        contenitore.getChildren().addAll(
                titolo, campoNome, campoUsername, campoPassword, campoConfermaPassword,
                btnConferma, btnAnnulla
        );
        return contenitore;
    }

    public void onConfermaRegistrazioneCliccato() {
        // TODO: validare i campi (password == conferma) e chiamare
        // ConnettoreServer.eseguiRegistrazione(nome, username, password).
        // In caso di successo, navigare al login o caricare direttamente la dashboard.
        String nome = campoNome.getText();
        String username = campoUsername.getText();
        System.out.println("TODO registrazione: " + nome + " / " + username);
    }

    public void onAnnullaCliccato() {
        gestoreScene.vaiAStart();
    }
}
