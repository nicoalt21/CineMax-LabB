package cinemax.client.gui.navigation;

import cinemax.client.controller.auth.StartController;
import cinemax.client.controller.auth.LoginController;
import cinemax.client.controller.auth.RegistrazioneController;
import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.common.model.Utente;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/*
  Gestore centrale della navigazione. Possiede lo Stage principale e si occupa di sostituire la radice della Scene per cambiare schermata
  riusa sempre lo stesso foglio di stile.

  Tutta la UI è costruita in codice Java: il gestore istanzia i controller (StartController, BaseLayoutController, ...) e inietta sé stesso
  dove serve per permettere la navigazione.
 */
public class GestoreScene {

    private Stage stagePrincipale;
    private Scene scena;

    // Inizializza il gestore con lo Stage principale e mostra la prima schermata (Start). Da chiamare una sola volta all'avvio.
    public void inizializza(Stage primaryStage) {
        this.stagePrincipale = primaryStage;
        vaiAStart();
    }

    // Mostra la schermata iniziale (Accedi / Registrati / Continua come Guest).
    public void vaiAStart() {
        StartController start = new StartController(this);
        impostaRadice(start.getRoot());
    }

    // Mostra la schermata di login.
    public void vaiALogin() {
        LoginController login = new LoginController(this);
        impostaRadice(login.getRoot());
    }

    // Mostra la schermata di registrazione.
    public void vaiARegistrazione() {
        RegistrazioneController registrazione = new RegistrazioneController(this);
        impostaRadice(registrazione.getRoot());
    }

    /*
      Carica il guscio (header + menu + area centrale) con la dashboard. Se utenteLoggato è null si entra in modalità Guest: stessa interfaccia
      del Client, ma con le parti riservate attenuate e non cliccabili.
    */
    public void caricaLayoutEDashboard(Utente utenteLoggato) {
        BaseLayoutController layout = new BaseLayoutController(this);
        layout.inizializzaContesto(utenteLoggato); // null = Guest
        // TODO: costruire e iniettare la dashboard del ruolo nell'area centrale:
        //   layout.impostaContenutoCentrale(dashboard.getRoot());
        impostaRadice(layout.getRoot());
    }

    public void mostraFinestraModale(String percorsoFxml, Object datiDaPassare) {
        // TODO: finestre modali (dettagli proiezione, conferme, ...).
    }

    // Sostituisce la radice della Scene. Crea la Scene al primo utilizzo, agganciando il foglio di stile, poi riusa sempre la stessa.
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
