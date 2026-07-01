package cinemax.client.gui.component.overlay;

import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.Film;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Overlay centrale coi dettagli di una prenotazione, estratto da BaseLayoutController.
 * <p>
 * È uno strato a tutta superficie (StackPane con sfondo scurito) che centra un riquadro
 * scrollabile coi dati della prenotazione col codice ben visibile e copiabile,
 * e i bottoni Annulla/Modifica.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class OverlayDettagliPrenotazione extends StackPane {

    private final VBox riquadroPrenotazione = new VBox(16);
    private final Label prenTitolo = new Label();
    private final Circle prenPallinoEta = new Circle(7);
    private final Label prenEta = new Label();
    private final Label prenDataOra = new Label();
    private final Label prenBiglietti = new Label();
    private final Label prenTotale = new Label();
    private final TextField prenCodice = new TextField();
    private final Button prenBtnAnnulla = new Button("Annulla prenotazione");
    private final Button prenBtnModifica = new Button("Mod. prenotazione");

    private Prenotazione prenotazioneDettaglio;
    private Consumer<Prenotazione> azioneAnnullaPrenotazione;
    private Consumer<Prenotazione> azioneModificaPrenotazione;

    /**
     * Costruisce l'interfaccia dell'overlay.
     */
    public OverlayDettagliPrenotazione() {
        prenTitolo.getStyleClass().add("titolo-principale");
        prenTitolo.setStyle("-fx-font-size: 24px;");
        prenTitolo.setWrapText(true);

        prenEta.getStyleClass().add("testo-secondario");
        HBox rigaEta = new HBox(8, prenPallinoEta, prenEta);
        rigaEta.setAlignment(Pos.CENTER_LEFT);

        Button btnChiudi = new Button("X");
        btnChiudi.getStyleClass().add("bottone-collassa");
        btnChiudi.setOnAction(e -> chiudi());
        Region spintaChiudi = new Region();
        HBox.setHgrow(spintaChiudi, Priority.ALWAYS);
        HBox rigaIntestazione = new HBox(10, prenTitolo, spintaChiudi, btnChiudi);
        rigaIntestazione.setAlignment(Pos.TOP_LEFT);

        prenDataOra.getStyleClass().add("testo-normale");
        prenDataOra.setWrapText(true);
        prenBiglietti.getStyleClass().add("testo-normale");
        prenBiglietti.setWrapText(true);
        prenTotale.getStyleClass().add("testo-normale");

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.getStyleClass().add("divisore-menu");

        Label etichettaCodice = new Label("Codice prenotazione");
        etichettaCodice.getStyleClass().add("etichetta-campo");
        prenCodice.setEditable(false);
        prenCodice.getStyleClass().add("campo-codice");
        prenCodice.setStyle("-fx-font-size: 14px;");
        VBox bloccoCodice = new VBox(4, etichettaCodice, prenCodice);

        prenBtnModifica.getStyleClass().add("bottone-primario");
        prenBtnModifica.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(prenBtnModifica, Priority.ALWAYS);
        prenBtnModifica.setOnAction(e -> {
            Prenotazione p = prenotazioneDettaglio;
            Consumer<Prenotazione> azione = azioneModificaPrenotazione;
            chiudi();
            if (p != null && azione != null) {
                azione.accept(p);
            }
        });

        prenBtnAnnulla.getStyleClass().add("bottone-secondario");
        prenBtnAnnulla.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(prenBtnAnnulla, Priority.ALWAYS);
        prenBtnAnnulla.setOnAction(e -> {
            Prenotazione p = prenotazioneDettaglio;
            Consumer<Prenotazione> azione = azioneAnnullaPrenotazione;
            chiudi();
            if (p != null && azione != null) {
                azione.accept(p);
            }
        });

        VBox rigaBottoni = new VBox(8, prenBtnModifica, prenBtnAnnulla);
        rigaBottoni.setAlignment(Pos.CENTER);

        riquadroPrenotazione.getStyleClass().add("riquadro-conferma");
        riquadroPrenotazione.setPadding(new Insets(16));
        riquadroPrenotazione.setPrefWidth(360);
        riquadroPrenotazione.setMaxWidth(360);
        riquadroPrenotazione.setMaxHeight(Region.USE_PREF_SIZE);
        riquadroPrenotazione.getChildren().addAll(
                rigaIntestazione, rigaEta, sep,
                bloccoCodice,
                prenDataOra, prenBiglietti, prenTotale,
                rigaBottoni);

        ScrollPane scroll = new ScrollPane(riquadroPrenotazione);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        scroll.setMaxWidth(360);
        scroll.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(scroll, Pos.CENTER);

        getStyleClass().add("overlay-conferma");
        setAlignment(Pos.CENTER);
        setOnMouseClicked(e -> chiudi());
        riquadroPrenotazione.setOnMouseClicked(javafx.event.Event::consume);
        getChildren().add(scroll);

        setVisible(false);
        setManaged(false);
    }

    /**
     * Mostra l'overlay coi dettagli di una prenotazione attiva.
     * * @param p La prenotazione da mostrare
     * @param onAnnulla Callback per l'annullamento
     * @param onModifica Callback per la modifica
     */
    public void mostra(Prenotazione p, Consumer<Prenotazione> onAnnulla, Consumer<Prenotazione> onModifica) {
        if (p == null) return;

        this.prenotazioneDettaglio = p;
        this.azioneAnnullaPrenotazione = onAnnulla;
        this.azioneModificaPrenotazione = onModifica;

        Proiezione proiezione = p.getProiezione();
        Film film = proiezione.getFilm();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

        prenTitolo.setText(film.getTitolo());

        FasciaEta.Fascia fascia = FasciaEta.fasciaPerEta(film.getEtaMinima());
        prenPallinoEta.getStyleClass().setAll("pallino-eta", fascia.getClasseCss());
        prenEta.setText(film.getEtaMinima() <= 0
                ? "Adatto a tutte le età!"
                : "Vietato ai minori di " + film.getEtaMinima() + " anni");

        prenDataOra.setText("Proiezione: " + proiezione.getDataOra().format(formato));
        int n = p.getNumeroBiglietti();
        prenBiglietti.setText(n + (n == 1 ? " biglietto" : " biglietti"));
        double totale = n * proiezione.getCostoBiglietto();
        prenTotale.setText(String.format("Totale: %.2f \u20ac", totale));
        prenCodice.setText(p.getCodice());

        setVisible(true);
        setManaged(true);
    }

    /**
     * Chiude l'overlay e resetta lo stato.
     */
    public void chiudi() {
        setVisible(false);
        setManaged(false);
        prenotazioneDettaglio = null;
        azioneAnnullaPrenotazione = null;
        azioneModificaPrenotazione = null;
    }
}