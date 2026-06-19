package cinemax.client.controller.auth;

import cinemax.client.gui.navigation.GestoreScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// Schermata di login, costruita interamente in codice Java (come StartController). Riceve il GestoreScene nel costruttore per poter navigare.
public class LoginController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    private final TextField campoUsername = new TextField();
    private final PasswordField campoPassword = new PasswordField();

    public LoginController(GestoreScene gestoreScene) {
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

        Label titolo = new Label("Accedi");
        titolo.getStyleClass().add("titolo-principale");

        campoUsername.setPromptText("Username");
        campoUsername.getStyleClass().add("campo-testo");
        campoUsername.setMaxWidth(280);

        campoPassword.setPromptText("Password");
        campoPassword.getStyleClass().add("campo-testo");
        campoPassword.setMaxWidth(280);

        Button btnConferma = new Button("Conferma");
        btnConferma.setMaxWidth(280);
        btnConferma.getStyleClass().add("bottone-primario");
        btnConferma.setOnAction(e -> onConfermaLoginCliccato());

        Button btnIndietro = new Button("Indietro");
        btnIndietro.setMaxWidth(280);
        btnIndietro.getStyleClass().add("bottone-secondario");
        btnIndietro.setOnAction(e -> onIndietroCliccato());

        // Riga "Oppure Registrati": testo statico + link cliccabile verso la registrazione.
        HBox rigaRegistrati = new HBox(4);
        rigaRegistrati.setAlignment(Pos.CENTER);
        Label etichettaOppure = new Label("Oppure");
        etichettaOppure.getStyleClass().add("testo-secondario");
        Hyperlink linkRegistrati = new Hyperlink("Registrati");
        linkRegistrati.getStyleClass().add("link-testuale");
        linkRegistrati.setOnAction(e -> onRegistratiCliccato());
        rigaRegistrati.getChildren().addAll(etichettaOppure, linkRegistrati);

        contenitore.getChildren().addAll(
                titolo, campoUsername, campoPassword,
                btnConferma, btnIndietro,
                rigaRegistrati
        );
        return contenitore;
    }

    public void onConfermaLoginCliccato() {
        // TODO: validare i campi e chiamare ConnettoreServer.eseguiLogin(Username, password).
        // In caso di successo il server restituisce l'Utente:
        //   Utente utente = ...;
        //   gestoreScene.caricaLayoutEDashboard(utente);
        String Username = campoUsername.getText();
        String password = campoPassword.getText();
        System.out.println("TODO login: " + Username + " / " + password);
    }

    public void onRegistratiCliccato() {
        gestoreScene.vaiARegistrazione();
    }

    public void onIndietroCliccato() {
        gestoreScene.vaiAStart();
    }
}
