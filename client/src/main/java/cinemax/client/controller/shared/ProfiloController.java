/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.controller.shared;

import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Utente;
import cinemax.common.util.Cifrario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/*
 Schermata "Profilo" comune a tutti i ruoli, costruita interamente in codice Java.

 Permette di modificare i dati personali (nome, cognome, data di nascita, domicilio) e,
 opzionalmente, la password. Username e ruolo non sono modificabili.

 Sicurezza: per salvare qualsiasi modifica l'utente deve re-inserire la password
 attuale, che viene confrontata (cifrata) con quella memorizzata.

 NOTA: le interfacce remote attuali non hanno un metodo per aggiornare un utente. Per
 ora il salvataggio aggiorna l'oggetto Utente in memoria (sufficiente con i servizi
 finti). Quando il team aggiungerà un metodo come ServizioAutenticazione.modificaUtente,
 basterà chiamarlo nel punto segnato più sotto.
 */
public class ProfiloController extends DashboardBaseController {

    private final GestoreScene gestoreScene;

    private final VBox radice = new VBox(15);

    private final TextField campoNome = new TextField();
    private final TextField campoCognome = new TextField();
    private final DatePicker campoDataNascita = new DatePicker();
    private final TextField campoDomicilio = new TextField();
    private final PasswordField campoNuovaPassword = new PasswordField();
    private final PasswordField campoPasswordAttuale = new PasswordField();
    private final Label labelMessaggio = new Label();

    public ProfiloController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titolo = new Label("Profilo");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        VBox modulo = new VBox(12);
        modulo.setMaxWidth(420);

        // Username e ruolo: mostrati ma non modificabili.
        Label infoAccount = new Label("Username: " + utenteLoggato.getUsername()
                + "   |   Ruolo: " + etichettaRuolo(utenteLoggato));
        infoAccount.getStyleClass().add("testo-secondario");

        modulo.getChildren().addAll(
                infoAccount,
                campo("Nome", campoNome),
                campo("Cognome", campoCognome),
                campo("Data di nascita", campoDataNascita),
                campo("Domicilio", campoDomicilio),
                campo("Nuova password (lascia vuoto per non cambiarla)", campoNuovaPassword)
        );

        // Separatore logico: conferma con password attuale.
        Label etichettaConferma = new Label("Per salvare, inserisci la password attuale:");
        etichettaConferma.getStyleClass().add("etichetta-campo");
        campoPasswordAttuale.setPromptText("Password attuale");
        campoPasswordAttuale.getStyleClass().add("campo-testo");
        campoPasswordAttuale.setMaxWidth(420);

        Button btnSalva = new Button("Salva modifiche");
        btnSalva.getStyleClass().add("bottone-primario");
        btnSalva.setOnAction(e -> salva());

        labelMessaggio.getStyleClass().add("campo-errore");
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);

        modulo.getChildren().addAll(etichettaConferma, campoPasswordAttuale, btnSalva, labelMessaggio);

        ScrollPane scroll = new ScrollPane(modulo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titolo, scroll);

        aggiornaDati();
    }

    @Override
    public void aggiornaDati() {
        // Precompila i campi con i dati attuali dell'utente.
        campoNome.setText(utenteLoggato.getNome());
        campoCognome.setText(utenteLoggato.getCognome());
        campoDataNascita.setValue(utenteLoggato.getDataNascita());
        campoDomicilio.setText(utenteLoggato.getLuogoDomicilio());
        campoNuovaPassword.clear();
        campoPasswordAttuale.clear();
        pulisciMessaggio();
    }

    private void salva() {
        pulisciMessaggio();

        // 1) Verifica della password attuale (obbligatoria per confermare).
        String passwordAttuale = campoPasswordAttuale.getText();
        if (passwordAttuale == null || passwordAttuale.isBlank()) {
            mostraErrore("Inserisci la password attuale per confermare.");
            return;
        }
        String hashInserito = Cifrario.cifraPassword(passwordAttuale);
        if (!hashInserito.equals(utenteLoggato.getPasswordCifrata())) {
            mostraErrore("Password attuale non corretta.");
            return;
        }

        // 2) Validazione minima dei campi.
        if (campoNome.getText().isBlank() || campoCognome.getText().isBlank()
                || campoDomicilio.getText().isBlank()) {
            mostraErrore("Nome, cognome e domicilio non possono essere vuoti.");
            return;
        }

        // 3) Applico le modifiche all'oggetto Utente.
        utenteLoggato.setNome(campoNome.getText().trim());
        utenteLoggato.setCognome(campoCognome.getText().trim());
        utenteLoggato.setDataNascita(campoDataNascita.getValue());
        utenteLoggato.setLuogoDomicilio(campoDomicilio.getText().trim());
        if (!campoNuovaPassword.getText().isBlank()) {
            utenteLoggato.setPasswordCifrata(Cifrario.cifraPassword(campoNuovaPassword.getText()));
        }

        // TODO (quando ci sarà la presa remota): inviare l'utente aggiornato al server,
        // es. gestoreScene.getFornitoreServizi().getServizioAutenticazione()
        //              .modificaUtente(utenteLoggato);
        // Per ora, con i servizi finti, la modifica in memoria è già effettiva.

        mostraSuccesso("Modifiche salvate.");
        campoNuovaPassword.clear();
        campoPasswordAttuale.clear();
    }

    // Riga "etichetta sopra, campo sotto" a larghezza fissa.
    private VBox campo(String etichetta, javafx.scene.control.Control controllo) {
        VBox colonna = new VBox(4);
        Label label = new Label(etichetta);
        label.getStyleClass().add("etichetta-campo");
        controllo.getStyleClass().add("campo-testo");
        controllo.setMaxWidth(420);
        colonna.getChildren().addAll(label, controllo);
        return colonna;
    }

    private String etichettaRuolo(Utente u) {
        switch (u.getRuolo()) {
            case PROIEZIONISTA: return "Proiezionista";
            case BIGLIETTAIO:   return "Bigliettaio";
            default:            return "Cliente";
        }
    }

    private void mostraErrore(String messaggio) {
        labelMessaggio.getStyleClass().setAll("campo-errore");
        labelMessaggio.setText(messaggio);
        labelMessaggio.setManaged(true);
        labelMessaggio.setVisible(true);
    }

    private void mostraSuccesso(String messaggio) {
        labelMessaggio.getStyleClass().setAll("testo-secondario");
        labelMessaggio.setText(messaggio);
        labelMessaggio.setManaged(true);
        labelMessaggio.setVisible(true);
    }

    private void pulisciMessaggio() {
        labelMessaggio.setText("");
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);
    }
}
