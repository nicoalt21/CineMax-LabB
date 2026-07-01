package cinemax.client.controller.shared;

import cinemax.client.gui.component.CampoConEtichetta;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Schermata "Profilo" comune a tutti i ruoli, costruita interamente in codice Java.
 * <p>
 * Permette di modificare i dati personali (nome, cognome, data di nascita, domicilio) e,
 * opzionalmente, la password. Username e ruolo non sono modificabili.
 * <p>
 * Sicurezza: per salvare qualsiasi modifica l'utente deve re-inserire la password
 * attuale, che viene confrontata (cifrata) con quella memorizzata.
 * <p>
 * Il salvataggio invia l'utente aggiornato al server tramite
 * ServizioAutenticazione.modificaUtente. Le modifiche vengono applicate all'oggetto in
 * memoria e, se il server rifiuta o non e' raggiungibile, vengono annullate (rollback) per
 * mantenere l'oggetto allineato allo stato remoto.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ProfiloController extends DashboardBaseController {

    private final GestoreScene gestoreScene;

    private final VBox radice = new VBox(15);

    // Larghezza dei campi e del blocco a due colonne, come nella schermata di registrazione.
    private static final double LARGHEZZA_CAMPO = 260;
    private static final double LARGHEZZA_GRIGLIA = LARGHEZZA_CAMPO * 2 + 24;

    private final CampoConEtichetta campoNome =
            new CampoConEtichetta("Nome", true, nuovoTextField("Nome"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoCognome =
            new CampoConEtichetta("Cognome", true, nuovoTextField("Cognome"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoDataNascita =
            new CampoConEtichetta("Data di nascita", false, new DatePicker(), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoDomicilio =
            new CampoConEtichetta("Domicilio", true, nuovoTextField("Domicilio"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoNuovaPassword =
            new CampoConEtichetta("Nuova password (vuoto = invariata)", false, new PasswordField(), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoPasswordAttuale =
            new CampoConEtichetta("Password attuale", true, new PasswordField(), LARGHEZZA_CAMPO);
    private final Label labelMessaggio = new Label();

    /**
     * Costruisce il controller per la gestione del profilo utente.
     *
     * @param gestoreScene Il gestore delle scene per l'accesso ai servizi.
     */
    public ProfiloController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
    }

    private static TextField nuovoTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    /**
     * Restituisce il nodo radice dell'interfaccia grafica.
     *
     * @return Il Parent radice.
     */
    @Override
    public Parent getRoot() {
        return radice;
    }

    /**
     * Inizializza i componenti grafici della schermata profilo.
     */
    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titolo = new Label("Profilo");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        VBox modulo = new VBox(12);
        modulo.setAlignment(Pos.CENTER);
        modulo.setMaxWidth(LARGHEZZA_GRIGLIA);

        // Username e ruolo: mostrati ma non modificabili.
        Label infoAccount = new Label("Username: " + utenteLoggato.getUsername()
                + "   |   Ruolo: " + etichettaRuolo(utenteLoggato));
        infoAccount.getStyleClass().add("testo-secondario");
        infoAccount.setMaxWidth(LARGHEZZA_GRIGLIA);

        labelMessaggio.getStyleClass().add("errore-generale");
        labelMessaggio.setWrapText(true);
        labelMessaggio.setMaxWidth(LARGHEZZA_GRIGLIA);
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);

        Button btnSalva = new Button("Salva modifiche");
        btnSalva.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnSalva.getStyleClass().add("bottone-primario");
        btnSalva.setOnAction(e -> salva());

        modulo.getChildren().addAll(
                infoAccount,
                costruisciGriglia(),
                labelMessaggio,
                btnSalva
        );

        ScrollPane scroll = new ScrollPane(modulo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titolo, scroll);

        aggiornaDati();
    }

    /**
     * Costruisce la griglia a due colonne per disporre i campi.
     *
     * @return Il pannello GridPane configurato.
     */
    private GridPane costruisciGriglia() {
        GridPane griglia = new GridPane();
        griglia.setAlignment(Pos.CENTER);
        griglia.setHgap(24);
        griglia.setVgap(12);
        griglia.setMaxWidth(LARGHEZZA_GRIGLIA);

        griglia.add(campoNome, 0, 0);
        griglia.add(campoCognome, 1, 0);

        griglia.add(campoDomicilio, 0, 1);
        griglia.add(campoDataNascita, 1, 1);

        Region stacco = new Region();
        stacco.setMinHeight(8);
        griglia.add(stacco, 0, 2);

        griglia.add(campoNuovaPassword, 0, 3);
        griglia.add(campoPasswordAttuale, 1, 3);

        return griglia;
    }

    /**
     * Riempie il modulo con i dati salvati dell'utente attualmente loggato.
     */
    @Override
    public void aggiornaDati() {
        ((TextField) campoNome.getControllo()).setText(utenteLoggato.getNome());
        ((TextField) campoCognome.getControllo()).setText(utenteLoggato.getCognome());
        ((DatePicker) campoDataNascita.getControllo()).setValue(utenteLoggato.getDataNascita());
        ((TextField) campoDomicilio.getControllo()).setText(utenteLoggato.getLuogoDomicilio());
        ((PasswordField) campoNuovaPassword.getControllo()).clear();
        ((PasswordField) campoPasswordAttuale.getControllo()).clear();
        pulisciMessaggio();
    }

    /**
     * Valida e salva i nuovi dati inseriti, previo inserimento della password attuale.
     */
    private void salva() {
        pulisciMessaggio();
        pulisciErroriCampi();

        // Verifica della password attuale (obbligatoria per confermare).
        String passwordAttuale = campoPasswordAttuale.getTesto();
        if (passwordAttuale == null || passwordAttuale.isBlank()) {
            campoPasswordAttuale.evidenziaErrore();
            mostraErrore("Inserisci la password attuale per confermare.");
            return;
        }
        String hashInserito = Cifrario.cifraPassword(passwordAttuale);
        if (!hashInserito.equals(utenteLoggato.getPasswordCifrata())) {
            campoPasswordAttuale.evidenziaErrore();
            mostraErrore("Password attuale non corretta.");
            return;
        }

        // Validazione dei campi obbligatori (Nome e Cognome).
        if (campoNome.getTesto().isBlank() || campoCognome.getTesto().isBlank()) {
            if (campoNome.getTesto().isBlank()) campoNome.evidenziaErrore();
            if (campoCognome.getTesto().isBlank()) campoCognome.evidenziaErrore();
            mostraErrore("Nome e cognome non possono essere vuoti.");
            return;
        }

        // Controllo caratteri validi per nome e cognome
        String regexNomi = "^[A-Za-zÀ-ÿ\\s\\-']+$";
        if (!campoNome.getTesto().matches(regexNomi) || !campoCognome.getTesto().matches(regexNomi)) {
            if (!campoNome.getTesto().matches(regexNomi)) campoNome.evidenziaErrore();
            if (!campoCognome.getTesto().matches(regexNomi)) campoCognome.evidenziaErrore();
            mostraErrore("Nome e cognome possono contenere solo lettere, spazi, apostrofi o trattini.");
            return;
        }

        // Validazione minima dei campi obbligatori (Nome e Cognome).
        if (campoNome.getTesto().isBlank() || campoCognome.getTesto().isBlank()) {
            if (campoNome.getTesto().isBlank()) campoNome.evidenziaErrore();
            if (campoCognome.getTesto().isBlank()) campoCognome.evidenziaErrore();
            mostraErrore("Nome e cognome non possono essere vuoti.");
            return;
        }

        // Validazione data di nascita (non nel futuro).
        java.time.LocalDate dataNascita = ((DatePicker) campoDataNascita.getControllo()).getValue();
        if (dataNascita != null && dataNascita.isAfter(java.time.LocalDate.now())) {
            campoDataNascita.evidenziaErrore();
            mostraErrore("La data di nascita non può essere nel futuro.");
            return;
        }

        // Validazione lunghezza nuova password (se l'utente vuole cambiarla).
        String nuovaPass = campoNuovaPassword.getTesto();
        if (!nuovaPass.isEmpty() && nuovaPass.length() < 5) {
            campoNuovaPassword.evidenziaErrore();
            mostraErrore("La nuova password deve avere almeno 5 caratteri.");
            return;
        }

        // Salvo i valori attuali per poter annullare le modifiche se il server rifiuta.
        String vecchioNome = utenteLoggato.getNome();
        String vecchioCognome = utenteLoggato.getCognome();
        java.time.LocalDate vecchiaData = utenteLoggato.getDataNascita();
        String vecchioDomicilio = utenteLoggato.getLuogoDomicilio();
        String vecchiaPassword = utenteLoggato.getPasswordCifrata();

        // Applico le modifiche all'oggetto Utente.
        utenteLoggato.setNome(campoNome.getTesto().trim());
        utenteLoggato.setCognome(campoCognome.getTesto().trim());
        utenteLoggato.setDataNascita(dataNascita);

        String domicilioInserito = campoDomicilio.getTesto().trim();
        utenteLoggato.setLuogoDomicilio(domicilioInserito.isEmpty() ? null : domicilioInserito);

        if (!nuovaPass.isEmpty()) {
            utenteLoggato.setPasswordCifrata(Cifrario.cifraPassword(nuovaPass));
        }

        // Invio l'utente aggiornato al server.
        try {
            boolean ok = gestoreScene.getFornitoreServizi()
                    .getServizioAutenticazione()
                    .modificaUtente(utenteLoggato);
            if (!ok) {
                ripristina(vecchioNome, vecchioCognome, vecchiaData, vecchioDomicilio, vecchiaPassword);
                mostraErrore("Il server ha rifiutato le modifiche. Riprova.");
                return;
            }
        } catch (java.rmi.RemoteException ex) {
            ripristina(vecchioNome, vecchioCognome, vecchiaData, vecchioDomicilio, vecchiaPassword);
            mostraErrore("Server non raggiungibile. Modifiche non salvate.");
            return;
        }

        mostraSuccesso("Modifiche salvate.");
        ((PasswordField) campoNuovaPassword.getControllo()).clear();
        ((PasswordField) campoPasswordAttuale.getControllo()).clear();
    }

    /**
     * Ripristina i dati dell'utente in memoria dopo un salvataggio fallito.
     */
    private void ripristina(String nome, String cognome, java.time.LocalDate data,
                            String domicilio, String password) {
        utenteLoggato.setNome(nome);
        utenteLoggato.setCognome(cognome);
        utenteLoggato.setDataNascita(data);
        utenteLoggato.setLuogoDomicilio(domicilio);
        utenteLoggato.setPasswordCifrata(password);
    }

    private void pulisciErroriCampi() {
        campoNome.pulisciErrore();
        campoCognome.pulisciErrore();
        campoDomicilio.pulisciErrore();
        campoDataNascita.pulisciErrore();
        campoNuovaPassword.pulisciErrore();
        campoPasswordAttuale.pulisciErrore();
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