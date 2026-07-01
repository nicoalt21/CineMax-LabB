package cinemax.client.controller.auth;

import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Utente;
import cinemax.common.util.Cifrario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;

/**
 * Schermata di login, costruita interamente in codice Java (come StartController).
 * Riceve il GestoreScene nel costruttore per poter navigare.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class LoginController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    private final TextField campoUsername = new TextField();
    private final PasswordField campoPassword = new PasswordField();
    private final Label labelErrore = new Label();

    /**
     * Costruisce la schermata di login.
     *
     * @param gestoreScene gestore di navigazione per passare alle altre schermate
     */
    public LoginController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    /** @return il nodo radice della schermata, da inserire nella Scene. */
    public VBox getRoot() {
        return radice;
    }

    /**
     * Costruisce la vista del login: campi username/password, bottoni e link.
     *
     * @return il contenitore radice della schermata
     */
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

        labelErrore.getStyleClass().add("campo-errore");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);

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
                btnConferma, labelErrore, btnIndietro,
                rigaRegistrati
        );
        return contenitore;
    }

    /**
     * Valida i campi, cifra la password e invia il login al server. In caso di successo
     * carica la dashboard adatta all'utente; altrimenti mostra un messaggio d'errore.
     * La password in chiaro non lascia mai il controller.
     */
    public void onConfermaLoginCliccato() {
        pulisciErrore();

        String username = campoUsername.getText();
        String password = campoPassword.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            mostraErrore("Inserisci username e password.");
            return;
        }

        // La password viaggia già cifrata: il chiaro non lascia mai il controller.
        String passwordCifrata = Cifrario.cifraPassword(password);

        try {
            Utente utente = gestoreScene.getFornitoreServizi()
                    .getServizioAutenticazione()
                    .login(username, passwordCifrata);

            if (utente == null) {
                mostraErrore("Username o password non corretti.");
                return;
            }

            // Login riuscito: entro nel layout con la dashboard adatta all'utente.
            gestoreScene.caricaLayoutEDashboard(utente);
        } catch (RemoteException e) {
            mostraErrore("Server non raggiungibile. Riprova.");
        }
    }

    /**
     * Mostra un messaggio d'errore sotto i campi.
     *
     * @param messaggio testo dell'errore da mostrare
     */
    private void mostraErrore(String messaggio) {
        labelErrore.setText(messaggio);
        labelErrore.setManaged(true);
        labelErrore.setVisible(true);
    }

    /** Nasconde e svuota il messaggio d'errore. */
    private void pulisciErrore() {
        labelErrore.setText("");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
    }

    /** Naviga alla schermata di registrazione. */
    public void onRegistratiCliccato() {
        gestoreScene.vaiARegistrazione();
    }

    /** Torna alla schermata iniziale (Start). */
    public void onIndietroCliccato() {
        gestoreScene.vaiAStart();
    }
}
