package cinemax.client.controller.proiezionista;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CampoConEtichetta;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Film;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;

/*
 Schermata "Crea film" del proiezionista, in una scheda distinta da "Crea proiezione" e
 con la stessa formattazione della schermata di registrazione (CampoConEtichetta in una
 griglia a due colonne).

 Il proiezionista inserisce i dati del film (titolo, genere, regista, anno, durata, età
 minima); alla conferma il film viene creato tramite ServizioProiezioni.aggiungiFilm. Se
 la creazione va a buon fine, il BaseLayoutController notifica l'esito all'utente e gli
 chiede se vuole creare anche una proiezione per quel film.
 */
public class CreaFilmController extends DashboardBaseController {

    private static final double LARGHEZZA_CAMPO = 260;
    private static final double LARGHEZZA_GRIGLIA = LARGHEZZA_CAMPO * 2 + 24;

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    private final VBox radice = new VBox(15);

    private final CampoConEtichetta campoTitolo =
            new CampoConEtichetta("Titolo", true, nuovoTextField("Titolo del film"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoGenere =
            new CampoConEtichetta("Genere", true, nuovoTextField("Es. Fantascienza"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoRegista =
            new CampoConEtichetta("Regista", true, nuovoTextField("Es. Christopher Nolan"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoAnno =
            new CampoConEtichetta("Anno", true, nuovoTextField("Es. 2024"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoDurata =
            new CampoConEtichetta("Durata (minuti)", true, nuovoTextField("Es. 120"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoEtaMinima =
            new CampoConEtichetta("Età minima", true, nuovoTextField("Es. 0, 14, 18"), LARGHEZZA_CAMPO);

    private final Label labelMessaggio = new Label();

    public CreaFilmController(GestoreScene gestoreScene, BaseLayoutController layout) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
    }

    private static TextField nuovoTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titolo = new Label("Crea film");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        VBox modulo = new VBox(12);
        modulo.setAlignment(Pos.CENTER);
        modulo.setMaxWidth(LARGHEZZA_GRIGLIA);

        labelMessaggio.getStyleClass().add("errore-generale");
        labelMessaggio.setWrapText(true);
        labelMessaggio.setMaxWidth(LARGHEZZA_GRIGLIA);
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);

        Button btnCrea = new Button("Crea film");
        btnCrea.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnCrea.getStyleClass().add("bottone-primario");
        btnCrea.setOnAction(e -> crea());

        Button btnPulisci = new Button("Pulisci campi");
        btnPulisci.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnPulisci.getStyleClass().add("bottone-secondario");
        btnPulisci.setOnAction(e -> aggiornaDati());

        modulo.getChildren().addAll(
                costruisciGriglia(),
                labelMessaggio,
                btnCrea,
                btnPulisci
        );

        ScrollPane scroll = new ScrollPane(modulo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titolo, scroll);

        aggiornaDati();
    }

    // Griglia a due colonne speculare alla schermata di registrazione.
    private GridPane costruisciGriglia() {
        GridPane griglia = new GridPane();
        griglia.setAlignment(Pos.CENTER);
        griglia.setHgap(24);
        griglia.setVgap(12);
        griglia.setMaxWidth(LARGHEZZA_GRIGLIA);

        griglia.add(campoTitolo, 0, 0);
        griglia.add(campoGenere, 1, 0);

        griglia.add(campoRegista, 0, 1);
        griglia.add(campoAnno, 1, 1);

        griglia.add(campoDurata, 0, 2);
        griglia.add(campoEtaMinima, 1, 2);

        Region stacco = new Region();
        stacco.setMinHeight(8);
        griglia.add(stacco, 0, 3);

        return griglia;
    }

    @Override
    public void aggiornaDati() {
        ((TextField) campoTitolo.getControllo()).clear();
        ((TextField) campoGenere.getControllo()).clear();
        ((TextField) campoRegista.getControllo()).clear();
        ((TextField) campoAnno.getControllo()).clear();
        ((TextField) campoDurata.getControllo()).clear();
        ((TextField) campoEtaMinima.getControllo()).clear();
        pulisciErroriCampi();
        pulisciMessaggio();
    }

    private void crea() {
        pulisciMessaggio();
        pulisciErroriCampi();

        boolean valido = true;

        String titolo = campoTitolo.getTesto().trim();
        if (titolo.isBlank()) {
            campoTitolo.evidenziaErrore();
            valido = false;
        }
        String genere = campoGenere.getTesto().trim();
        if (genere.isBlank()) {
            campoGenere.evidenziaErrore();
            valido = false;
        }
        String regista = campoRegista.getTesto().trim();
        if (regista.isBlank()) {
            campoRegista.evidenziaErrore();
            valido = false;
        }

        Integer anno = leggiIntero(campoAnno.getTesto());
        if (anno == null) {
            campoAnno.evidenziaErrore();
            valido = false;
        }
        Integer durata = leggiIntero(campoDurata.getTesto());
        if (durata == null || durata <= 0) {
            campoDurata.evidenziaErrore();
            valido = false;
        }
        Integer etaMinima = leggiIntero(campoEtaMinima.getTesto());
        if (etaMinima == null || etaMinima < 0) {
            campoEtaMinima.evidenziaErrore();
            valido = false;
        }

        if (!valido) {
            mostraErrore("Compila correttamente tutti i campi (anno, durata ed età devono essere numeri).");
            return;
        }

        // idFilm a 0: il servizio finto assegna l'id progressivo.
        Film nuovo = new Film(0, titolo, genere, regista, anno, durata, etaMinima);

        try {
            boolean ok = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni().aggiungiFilm(nuovo);
            if (ok) {
                aggiornaDati();
                // Notifica e proposta di creare anche una proiezione (gestita dal layout).
                layout.notificaFilmCreato(titolo);
            } else {
                campoTitolo.evidenziaErrore();
                mostraErrore("Esiste già un film identico (stessi titolo, genere, regista, "
                        + "anno, durata ed eta' minima). Modifica almeno un campo per crearlo.");
            }
        } catch (RemoteException ex) {
            mostraErrore("Errore di comunicazione col server: " + ex.getMessage());
        }
    }

    private Integer leggiIntero(String testo) {
        if (testo == null || testo.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(testo.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void pulisciErroriCampi() {
        campoTitolo.pulisciErrore();
        campoGenere.pulisciErrore();
        campoRegista.pulisciErrore();
        campoAnno.pulisciErrore();
        campoDurata.pulisciErrore();
        campoEtaMinima.pulisciErrore();
    }

    private void mostraErrore(String messaggio) {
        labelMessaggio.getStyleClass().setAll("errore-generale");
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
