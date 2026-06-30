package cinemax.client.controller.auth;

import cinemax.client.gui.navigation.GestoreScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/* Schermata iniziale, mostra i pulsanti Accedi / Registrati e l'ingresso in modalità Guest.
 Riceve il GestioreScene nel costruttore per poter navigare verso le altre schermate.
 */
public class StartController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    public StartController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    // Restituisce il nodo radice della schermata, da inserire nella Scene.
    public VBox getRoot() {
        return radice;
    }

    private VBox costruisciVista() {
        VBox contenitore = new VBox(30);
        contenitore.setAlignment(Pos.CENTER);
        contenitore.getStyleClass().add("sfondo-principale");
        contenitore.setPadding(new Insets(40));

        Label titolo = new Label("🎬 CineMax");
        titolo.getStyleClass().add("titolo-principale");

        Label sottotitolo = new Label("Prenota il tuo posto in prima fila.");
        sottotitolo.getStyleClass().add("testo-secondario");

        // Gruppo bottoni principali
        VBox gruppoBottoni = new VBox(15);
        gruppoBottoni.setAlignment(Pos.CENTER);

        Button btnAccedi = new Button("Accedi");
        btnAccedi.setMaxWidth(200);
        btnAccedi.getStyleClass().add("bottone-primario");
        btnAccedi.setOnAction(e -> onLoginCliccato());

        Button btnRegistrati = new Button("Registrati");
        btnRegistrati.setMaxWidth(200);
        btnRegistrati.getStyleClass().add("bottone-secondario");
        btnRegistrati.setOnAction(e -> onRegistratiCliccato());

        gruppoBottoni.getChildren().addAll(btnAccedi, btnRegistrati);

        // Separatore "oppure"
        HBox separatore = new HBox(10);
        separatore.setAlignment(Pos.CENTER);
        Label oppure = new Label("oppure");
        oppure.getStyleClass().add("testo-secondario");
        separatore.getChildren().add(oppure);

        // Ingresso Guest: stile secondario senza bordo, testo color primario
        Button btnGuest = new Button("Continua come Guest");
        btnGuest.getStyleClass().add("bottone-secondario");
        btnGuest.setStyle("-fx-border-color: transparent; -fx-text-fill: -fx-primary-color;");
        btnGuest.setOnAction(e -> onContinuaComeGuestCliccato());

        contenitore.getChildren().addAll(titolo, sottotitolo, gruppoBottoni, separatore, btnGuest);
        return contenitore;
    }

    public void onLoginCliccato() {
        gestoreScene.vaiALogin();
    }

    public void onRegistratiCliccato() {
        gestoreScene.vaiARegistrazione();
    }

    public void onContinuaComeGuestCliccato() {
        // Modalità Guest: nessun utente autenticato.
        gestoreScene.caricaLayoutEDashboard(null);
    }
}
