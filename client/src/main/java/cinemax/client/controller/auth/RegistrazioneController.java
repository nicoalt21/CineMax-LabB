package cinemax.client.controller.auth;

import cinemax.client.gui.component.CampoConEtichetta;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Ruolo;
import cinemax.common.model.Utente;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/*
 Schermata di registrazione, costruita interamente in codice Java (come StartController).
 Riceve il GestoreScene nel costruttore per poter navigare.

 La UI è composta solo da componenti riutilizzabili (CampoConEtichetta) e da classi CSS
 condivise: nessun testo, larghezza o stile è hardcodato nei singoli campi.

 Campi obbligatori (asterisco rosso): nome, cognome, username, password, conferma password.
 Campi facoltativi: data di nascita, domicilio.
 Il ruolo del nuovo utente è sempre CLIENTE.
 */
public class RegistrazioneController {

    // Costanti
    private static final double LARGHEZZA_CAMPO = 260;
    private static final int LUNGHEZZA_MINIMA_PASSWORD = 5;
    private static final Ruolo RUOLO_PREDEFINITO = Ruolo.CLIENTE;

    // Larghezza dell'intero blocco a due colonne (campo + spazio + campo)
    private static final double LARGHEZZA_GRIGLIA = LARGHEZZA_CAMPO * 2 + 24;

    private final GestoreScene gestoreScene;
    private final VBox radice;

    // Campi del form (incapsulati nel componente riutilizzabile)
    private final CampoConEtichetta campoNome =
            new CampoConEtichetta("Nome", true, nuovoTextField("Nome"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoCognome =
            new CampoConEtichetta("Cognome", true, nuovoTextField("Cognome"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoUsername =
            new CampoConEtichetta("Username", true, nuovoTextField("Username"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoPassword =
            new CampoConEtichetta("Password", true, new PasswordField(), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoConfermaPassword =
            new CampoConEtichetta("Conferma password", true, new PasswordField(), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoDataNascita =
            new CampoConEtichetta("Data di nascita", false, new DatePicker(), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoDomicilio =
            new CampoConEtichetta("Domicilio", false, nuovoTextField("Domicilio"), LARGHEZZA_CAMPO);

    // Messaggio di errore generale, mostrato in basso sopra il bottone Conferma
    private final Label labelErroreGenerale = new Label();

    public RegistrazioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    // Restituisce il nodo radice della schermata, da inserire nella Scene
    public VBox getRoot() {
        return radice;
    }

    private static TextField nuovoTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    private VBox costruisciVista() {
        VBox contenitore = new VBox(12);
        contenitore.setAlignment(Pos.CENTER);
        contenitore.getStyleClass().add("sfondo-principale");
        contenitore.setPadding(new Insets(40));

        Label titolo = new Label("Registrati");
        titolo.getStyleClass().add("titolo-principale");

        labelErroreGenerale.getStyleClass().add("errore-generale");
        labelErroreGenerale.setWrapText(true);
        labelErroreGenerale.setMaxWidth(LARGHEZZA_GRIGLIA);
        labelErroreGenerale.setManaged(false);
        labelErroreGenerale.setVisible(false);

        Button btnConferma = new Button("Conferma");
        btnConferma.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnConferma.getStyleClass().add("bottone-primario");
        btnConferma.setOnAction(e -> onConfermaRegistrazioneCliccato());

        Button btnAnnulla = new Button("Annulla");
        btnAnnulla.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnAnnulla.getStyleClass().add("bottone-secondario");
        btnAnnulla.setOnAction(e -> onAnnullaCliccato());

        Region spaziatore = new Region();
        spaziatore.setMinHeight(20);

        // Riga "Hai gia' un account? Accedi": rimanda alla schermata di login,
        // speculare al link "Oppure Registrati" presente nel LoginController.
        HBox rigaAccedi = new HBox(4);
        rigaAccedi.setAlignment(Pos.CENTER);
        Label etichettaAccount = new Label("Hai gia' un account?");
        etichettaAccount.getStyleClass().add("testo-secondario");
        Hyperlink linkAccedi = new Hyperlink("Accedi");
        linkAccedi.getStyleClass().add("link-testuale");
        linkAccedi.setOnAction(e -> onAccediCliccato());
        rigaAccedi.getChildren().addAll(etichettaAccount, linkAccedi);

        contenitore.getChildren().addAll(
                titolo,
                costruisciGriglia(),
                spaziatore,
                labelErroreGenerale,
                btnConferma, btnAnnulla,
                rigaAccedi
        );
        return contenitore;
    }

    private GridPane costruisciGriglia() {
        GridPane griglia = new GridPane();
        griglia.setAlignment(Pos.CENTER);
        griglia.setHgap(24);
        griglia.setVgap(12);
        griglia.setMaxWidth(LARGHEZZA_GRIGLIA);

        griglia.add(campoNome, 0, 0);
        griglia.add(campoUsername, 0, 1);
        griglia.add(campoDataNascita, 0, 2);

        griglia.add(campoCognome, 1, 0);
        griglia.add(campoDomicilio, 1, 1);

        Region staccoPassword = new Region();
        staccoPassword.setMinHeight(8);
        griglia.add(staccoPassword, 0, 3);

        griglia.add(campoPassword, 0, 4);
        griglia.add(campoConfermaPassword, 1, 4);

        return griglia;
    }

    public void onConfermaRegistrazioneCliccato() {
        pulisciErrori();

        boolean valido = true;

        if (isVuoto(campoNome.getTesto())) {
            campoNome.evidenziaErrore();
            valido = false;
        }
        if (isVuoto(campoCognome.getTesto())) {
            campoCognome.evidenziaErrore();
            valido = false;
        }
        if (isVuoto(campoUsername.getTesto())) {
            campoUsername.evidenziaErrore();
            valido = false;
        }
        if (isVuoto(campoPassword.getTesto())) {
            campoPassword.evidenziaErrore();
            valido = false;
        }
        if (isVuoto(campoConfermaPassword.getTesto())) {
            campoConfermaPassword.evidenziaErrore();
            valido = false;
        }
        if (!valido) {
            mostraErroreGenerale("Compila tutti i campi obbligatori (*).");
            return;
        }

        // Password troppo corta
        if (campoPassword.getTesto().length() < LUNGHEZZA_MINIMA_PASSWORD) {
            campoPassword.evidenziaErrore();
            campoConfermaPassword.evidenziaErrore();
            mostraErroreGenerale("La password deve avere almeno "
                    + LUNGHEZZA_MINIMA_PASSWORD + " caratteri.");
            return;
        }

        // Password non coincidenti
        if (!campoPassword.getTesto().equals(campoConfermaPassword.getTesto())) {
            campoPassword.evidenziaErrore();
            campoConfermaPassword.evidenziaErrore();
            mostraErroreGenerale("Le due password non sono uguali.");
            return;
        }

        // Tutti i controlli locali superati: costruisco l'oggetto Utente
        String nome = campoNome.getTesto().trim();
        String cognome = campoCognome.getTesto().trim();
        String username = campoUsername.getTesto().trim();
        String password = campoPassword.getTesto(); // <---------------------------------------------- QUI LA PASSWORD VA CIFRATA
        LocalDate dataNascita = leggiDataNascita();
        String domicilio = vuotoComeNull(campoDomicilio.getTesto());

        Utente nuovoUtente = new Utente(
                nome, cognome, username,
                password,
                dataNascita,       // può essere null
                domicilio,         // può essere null
                RUOLO_PREDEFINITO
        );

        // TODO: invio al server, es. ConnettoreServer.eseguiRegistrazione(nuovoUtente).
        // Il server restituisce esito; se l'username è già in uso, chiamare:
        //   mostraErroreUsernameInUso();
        // In caso di successo, navigare al login o caricare la dashboard:
        //   gestoreScene.vaiALogin();
        System.out.println("TODO registrazione utente: " + nuovoUtente.getUsername()
                + " (" + nuovoUtente.getRuolo() + ")");
    }

    // Da invocare quando il server segnala che lo username è già registrato.
    public void mostraErroreUsernameInUso() {
        campoUsername.evidenziaErrore();
        mostraErroreGenerale("Username già in uso.");
    }

    public void onAnnullaCliccato() {
        gestoreScene.vaiAStart();
    }

    // Naviga alla schermata di login (link "Hai già un account? Accedi").
    public void onAccediCliccato() {
        gestoreScene.vaiALogin();
    }

    // Helper privati
    private LocalDate leggiDataNascita() {
        DatePicker dp = (DatePicker) campoDataNascita.getControllo();
        return dp.getValue(); // null se non selezionata (campo facoltativo)
    }

    private void mostraErroreGenerale(String messaggio) {
        labelErroreGenerale.setText(messaggio);
        labelErroreGenerale.setManaged(true);
        labelErroreGenerale.setVisible(true);
    }

    private void pulisciErrori() {
        campoNome.pulisciErrore();
        campoCognome.pulisciErrore();
        campoUsername.pulisciErrore();
        campoPassword.pulisciErrore();
        campoConfermaPassword.pulisciErrore();
        campoDataNascita.pulisciErrore();
        campoDomicilio.pulisciErrore();
        labelErroreGenerale.setText("");
        labelErroreGenerale.setManaged(false);
        labelErroreGenerale.setVisible(false);
    }

    private static boolean isVuoto(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String vuotoComeNull(String s) {
        return isVuoto(s) ? null : s.trim();
    }
}
