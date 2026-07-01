package cinemax.client.controller.auth;

import cinemax.client.gui.component.CampoConEtichetta;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Ruolo;
import cinemax.common.model.Utente;
import cinemax.common.util.Cifrario;
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

/**
 * Schermata di registrazione, costruita interamente in codice Java (come StartController).
 * Riceve il GestoreScene nel costruttore per poter navigare.
 * <p>
 * La UI è composta solo da componenti riutilizzabili (CampoConEtichetta) e da classi CSS
 * condivise: nessun testo, larghezza o stile è hardcodato nei singoli campi.
 * <p>
 * Campi obbligatori (asterisco rosso): nome, cognome, username, password, conferma
 * password. Campi facoltativi: data di nascita, domicilio. Il ruolo del nuovo utente è
 * sempre CLIENTE.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
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

    /**
     * Costruisce la schermata di registrazione.
     *
     * @param gestoreScene gestore di navigazione per passare alle altre schermate
     */
    public RegistrazioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    /** @return il nodo radice della schermata, da inserire nella Scene. */
    public VBox getRoot() {
        return radice;
    }

    /**
     * Crea un TextField con il prompt indicato.
     *
     * @param prompt testo segnaposto del campo
     * @return il TextField configurato
     */
    private static TextField nuovoTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    /**
     * Costruisce la vista completa della registrazione (griglia campi, bottoni, link).
     *
     * @return il contenitore radice della schermata
     */
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

    /**
     * Costruisce la griglia a due colonne con i campi del form.
     *
     * @return la griglia dei campi
     */
    private GridPane costruisciGriglia() {
        GridPane griglia = new GridPane();
        griglia.setAlignment(Pos.CENTER);
        griglia.setHgap(24);
        griglia.setVgap(12);
        griglia.setMaxWidth(LARGHEZZA_GRIGLIA);

        griglia.add(campoNome, 0, 0);
        griglia.add(campoUsername, 0, 1);
        griglia.add(campoDomicilio, 0, 2);

        griglia.add(campoCognome, 1, 0);
        griglia.add(campoDataNascita, 1, 1);

        Region staccoPassword = new Region();
        staccoPassword.setMinHeight(8);
        griglia.add(staccoPassword, 0, 3);

        griglia.add(campoPassword, 0, 4);
        griglia.add(campoConfermaPassword, 1, 4);

        return griglia;
    }

    /**
     * Valida i campi (obbligatori, lunghezza e coincidenza password) e, se tutto è
     * corretto, costruisce l'Utente e lo invia al server con registraCliente.
     */
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

        // Validazione della data di nascita
        LocalDate dataNascita = leggiDataNascita();
        if (dataNascita != null && dataNascita.isAfter(LocalDate.now())) {
            campoDataNascita.evidenziaErrore();
            mostraErroreGenerale("La data di nascita non può essere nel futuro.");
            return;
        }

        // Controllo che nome e cognome contengano solo lettere, spazi, apostrofi o trattini
        String regexNomi = "^[A-Za-zÀ-ÿ\\s\\-']+$";

        if (!campoNome.getTesto().matches(regexNomi)) {
            campoNome.evidenziaErrore();
            valido = false;
        }
        if (!campoCognome.getTesto().matches(regexNomi)) {
            campoCognome.evidenziaErrore();
            valido = false;
        }

        if (!valido) {
            mostraErroreGenerale("Nome e cognome possono contenere solo lettere, spazi, apostrofi o trattini.");
            return;
        }

        // Tutti i controlli locali superati: costruisco l'oggetto Utente
        String nome = campoNome.getTesto().trim();
        String cognome = campoCognome.getTesto().trim();
        String username = campoUsername.getTesto().trim();
        String password = Cifrario.cifraPassword(campoPassword.getTesto());
        String domicilio = vuotoComeNull(campoDomicilio.getTesto());

        Utente nuovoUtente = new Utente(
                nome, cognome, username,
                password,
                dataNascita,
                domicilio,
                RUOLO_PREDEFINITO
        );

        try {
            boolean registrato = gestoreScene.getFornitoreServizi()
                    .getServizioAutenticazione()
                    .registraCliente(nuovoUtente);
            if (registrato) {
                gestoreScene.vaiALogin();
            } else {
                mostraErroreUsernameInUso();
            }
        } catch (java.rmi.RemoteException ex) {
            mostraErroreGenerale("Server non raggiungibile. Riprova piu' tardi.");
        }
    }

    /** Segnala che lo username scelto è già registrato, evidenziando il campo. */
    public void mostraErroreUsernameInUso() {
        campoUsername.evidenziaErrore();
        mostraErroreGenerale("Username già in uso.");
    }

    /** Annulla la registrazione e torna alla schermata iniziale (Start). */
    public void onAnnullaCliccato() {
        gestoreScene.vaiAStart();
    }

    /** Naviga alla schermata di login (link "Hai già un account? Accedi"). */
    public void onAccediCliccato() {
        gestoreScene.vaiALogin();
    }

    // Helper privati

    /**
     * Legge la data di nascita dal DatePicker.
     *
     * @return la data selezionata, oppure null se non indicata (campo facoltativo)
     */
    private LocalDate leggiDataNascita() {
        DatePicker dp = (DatePicker) campoDataNascita.getControllo();
        return dp.getValue(); // null se non selezionata (campo facoltativo)
    }

    /**
     * Mostra il messaggio di errore generale sopra il bottone Conferma.
     *
     * @param messaggio testo dell'errore da mostrare
     */
    private void mostraErroreGenerale(String messaggio) {
        labelErroreGenerale.setText(messaggio);
        labelErroreGenerale.setManaged(true);
        labelErroreGenerale.setVisible(true);
    }

    /** Ripulisce l'evidenziazione di tutti i campi e nasconde l'errore generale. */
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

    /**
     * @param s stringa da controllare
     * @return true se la stringa è null o composta di soli spazi
     */
    private static boolean isVuoto(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * @param s stringa da normalizzare
     * @return la stringa senza spazi ai lati, oppure null se vuota
     */
    private static String vuotoComeNull(String s) {
        return isVuoto(s) ? null : s.trim();
    }
}
