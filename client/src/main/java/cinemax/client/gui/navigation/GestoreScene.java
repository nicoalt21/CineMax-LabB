package cinemax.client.gui.navigation;

import cinemax.client.controller.auth.StartController;
import cinemax.client.controller.auth.ConnessioneController;
import cinemax.client.controller.auth.LoginController;
import cinemax.client.controller.auth.RegistrazioneController;
import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.gui.model.Impostazioni;
import cinemax.client.service.FornitoreServizi;
import cinemax.common.model.Utente;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Gestore centrale della navigazione. Possiede lo Stage principale e sostituisce la
 * radice della Scene per cambiare schermata, riusando sempre lo stesso foglio di stile.
 * <p>
 * Tutta la UI è costruita in codice Java: il gestore istanzia i controller
 * (StartController, BaseLayoutController, ...) e inietta se stesso dove serve per
 * permettere la navigazione.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class GestoreScene {

    private Stage stagePrincipale;
    private Scene scena;

    // Fornitore dei tre servizi remoti (oggi finti, domani stub RMI reali).
    // I controller lo ottengono da qui tramite getFornitoreServizi().
    private FornitoreServizi fornitoreServizi;

    // Impostazioni condivise per tutta la sessione (es. risultati per pagina).
    private final Impostazioni impostazioni = new Impostazioni();

    /**
     * Inizializza il gestore con lo Stage principale e il fornitore dei servizi, poi
     * mostra la prima schermata (connessione). Da chiamare una sola volta all'avvio.
     *
     * @param primaryStage     finestra principale JavaFX
     * @param fornitoreServizi fornitore dei servizi remoti (può essere null all'avvio)
     */
    public void inizializza(Stage primaryStage, FornitoreServizi fornitoreServizi) {
        this.stagePrincipale = primaryStage;
        this.fornitoreServizi = fornitoreServizi;
        vaiAConnessione();
    }

    /** @return il fornitore dei servizi remoti, da passare ai controller. */
    public FornitoreServizi getFornitoreServizi() {
        return fornitoreServizi;
    }

    /**
     * Imposta (o sostituisce) il fornitore dei servizi remoti. Usato dalla schermata di
     * connessione: all'avvio il fornitore è null e viene valorizzato qui solo quando la
     * connessione al server riesce davvero.
     *
     * @param fornitoreServizi nuovo fornitore dei servizi remoti
     */
    public void setFornitoreServizi(FornitoreServizi fornitoreServizi) {
        this.fornitoreServizi = fornitoreServizi;
    }

    /** @return le impostazioni condivise della sessione (es. risultati per pagina). */
    public Impostazioni getImpostazioni() {
        return impostazioni;
    }

    /** Mostra la schermata di connessione al server (prima vista all'avvio). */
    public void vaiAConnessione() {
        ConnessioneController connessione = new ConnessioneController(this);
        impostaRadice(connessione.getRoot());
    }

    /** Mostra la schermata iniziale (Accedi / Registrati / Continua come Guest). */
    public void vaiAStart() {
        StartController start = new StartController(this);
        impostaRadice(start.getRoot());
    }

    /** Mostra la schermata di login. */
    public void vaiALogin() {
        LoginController login = new LoginController(this);
        impostaRadice(login.getRoot());
    }

    /** Mostra la schermata di registrazione. */
    public void vaiARegistrazione() {
        RegistrazioneController registrazione = new RegistrazioneController(this);
        impostaRadice(registrazione.getRoot());
    }

    /**
     * Carica il guscio (header + menu + area centrale) con la dashboard. Se utenteLoggato
     * è null si entra in modalità Guest: stessa interfaccia del Client, ma con le parti
     * riservate attenuate e non cliccabili.
     *
     * @param utenteLoggato utente autenticato, oppure null per la modalità Guest
     */
    public void caricaLayoutEDashboard(Utente utenteLoggato) {
        caricaLayoutEDashboard(utenteLoggato, null);
    }

    /**
     * Variante che accetta un titolo iniziale: usata quando il Guest entra dal menu
     * iniziale indicando il nome (anche parziale) di un film. La dashboard precompila la
     * ricerca e mostra subito le proiezioni di quel film nei tre mesi successivi.
     *
     * @param utenteLoggato       utente autenticato, oppure null per la modalità Guest
     * @param titoloInizialeGuest titolo con cui precompilare la ricerca (può essere null)
     */
    public void caricaLayoutEDashboard(Utente utenteLoggato, String titoloInizialeGuest) {
        BaseLayoutController layout = new BaseLayoutController(this);
        layout.inizializzaContesto(utenteLoggato); // null = Guest

        // Il layout conosce il ruolo e sceglie la schermata iniziale adatta
        // (ricerca per cliente/guest/proiezionista, proiezioni di oggi per bigliettaio).
        layout.mostraDashboardIniziale(titoloInizialeGuest);

        impostaRadice(layout.getRoot());
    }

    /**
     * Sostituisce la radice della Scene. Crea la Scene al primo utilizzo, agganciando il
     * foglio di stile, poi riusa sempre la stessa.
     *
     * @param radice nuova radice da mostrare nella finestra
     */
    private void impostaRadice(Parent radice) {
        if (scena == null) {
            scena = new Scene(radice, 800, 600);
            URL cssLocation = getClass().getResource("/css/theme.css");
            if (cssLocation != null) {
                scena.getStylesheets().add(cssLocation.toExternalForm());
            } else {
                System.err.println("Attenzione: theme.css non trovato!");
            }
            stagePrincipale.setScene(scena);
        } else {
            scena.setRoot(radice);
        }
    }
}
