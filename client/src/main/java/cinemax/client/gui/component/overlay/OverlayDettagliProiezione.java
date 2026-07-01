package cinemax.client.gui.component.overlay;

import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/*
 Overlay centrale coi dettagli di una proiezione, estratto da BaseLayoutController.

 E' uno strato a tutta superficie (StackPane con sfondo scurito) che centra un riquadro
 scrollabile coi dati del film e della proiezione. Cliccando sullo sfondo (fuori dal
 riquadro) il pannello si chiude.

 Il componente NON conosce lo stato globale del     l'applicazione né la navigazione: tutto ciò
 che gli serve per decidere lo stato del bottone "Prenota" (se l'utente è guest, se è un
 proiezionista, la sua data di nascita) gli viene passato al momento dell'apertura, così
 come la callback da eseguire quando si preme "Prenota". Questo lo rende autonomo e
 testabile.
 */
public class OverlayDettagliProiezione extends StackPane {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final Label titolo = new Label();
    private final Circle pallinoEta = new Circle(7);
    private final Label eta = new Label();
    private final Label genere = new Label();
    private final Label regista = new Label();
    private final Label anno = new Label();
    private final Label durata = new Label();
    private final Label dataOra = new Label();
    private final Label fine = new Label();
    private final Label prezzo = new Label();
    private final Label posti = new Label();
    private final Label avviso = new Label();
    private final Button btnPrenota = new Button("Prenota");

    // Proiezione attualmente mostrata e callback di prenotazione fornita dal chiamante.
    private Proiezione proiezioneCorrente;
    private Consumer<Proiezione> onPrenota;

    public OverlayDettagliProiezione() {
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 24px;");
        titolo.setWrapText(true);

        eta.getStyleClass().add("testo-secondario");
        HBox rigaEta = new HBox(8, pallinoEta, eta);
        rigaEta.setAlignment(Pos.CENTER_LEFT);

        Button btnChiudi = new Button("X");
        btnChiudi.getStyleClass().add("bottone-collassa");
        btnChiudi.setOnAction(e -> chiudi());
        Region spintaChiudi = new Region();
        HBox.setHgrow(spintaChiudi, Priority.ALWAYS);
        HBox rigaIntestazione = new HBox(10, titolo, spintaChiudi, btnChiudi);
        rigaIntestazione.setAlignment(Pos.TOP_LEFT);

        for (Label l : new Label[]{genere, regista, anno, durata, dataOra, fine, prezzo, posti}) {
            l.getStyleClass().add("testo-normale");
            l.setWrapText(true);
        }

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.getStyleClass().add("divisore-menu");

        avviso.getStyleClass().add("testo-secondario");
        avviso.setWrapText(true);
        avviso.setManaged(false);
        avviso.setVisible(false);

        btnPrenota.getStyleClass().add("bottone-primario");
        btnPrenota.setMaxWidth(Double.MAX_VALUE);
        btnPrenota.setOnAction(e -> {
            Proiezione p = proiezioneCorrente;
            Consumer<Proiezione> azione = onPrenota;
            chiudi();
            if (p != null && azione != null) {
                azione.accept(p);
            }
        });

        VBox riquadro = new VBox(16);
        riquadro.getStyleClass().add("riquadro-conferma");
        riquadro.setPadding(new Insets(16));
        riquadro.setPrefWidth(360);
        riquadro.setMaxWidth(360);
        riquadro.setMaxHeight(Region.USE_PREF_SIZE);
        riquadro.getChildren().addAll(
                rigaIntestazione, rigaEta, sep,
                genere, regista, anno, durata,
                dataOra, fine, prezzo, posti,
                avviso, btnPrenota);

        ScrollPane scroll = new ScrollPane(riquadro);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        scroll.setMaxWidth(360);
        scroll.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(scroll, Pos.CENTER);

        getStyleClass().add("overlay-conferma");
        setAlignment(Pos.CENTER);
        // Click sullo sfondo scurito chiude; il click sul riquadro viene consumato.
        setOnMouseClicked(e -> chiudi());
        riquadro.setOnMouseClicked(javafx.event.Event::consume);
        getChildren().add(scroll);

        setVisible(false);
        setManaged(false);
    }

    /*
     Mostra i dettagli della proiezione indicata. I parametri di contesto (isGuest,
     isProiezionista, dataNascita) servono solo a decidere stato/avviso del bottone
     "Prenota"; onPrenota è la callback eseguita quando l'utente idoneo preme "Prenota".
     Consentito anche al Guest (sola consultazione).
     */
    public void mostra(Proiezione p, boolean isGuest, boolean isProiezionista,
                       LocalDate dataNascita, Consumer<Proiezione> onPrenota) {
        if (p == null) {
            return;
        }
        this.proiezioneCorrente = p;
        this.onPrenota = onPrenota;

        Film film = p.getFilm();
        titolo.setText(film.getTitolo());

        FasciaEta.Fascia fascia = FasciaEta.fasciaPerEta(film.getEtaMinima());
        pallinoEta.getStyleClass().setAll("pallino-eta", fascia.getClasseCss());
        eta.setText(film.getEtaMinima() <= 0
                ? "Adatto a tutte le età!"
                : "Vietato ai minori di " + film.getEtaMinima() + " anni");

        genere.setText("Genere: " + film.getGenere());
        regista.setText("Regista: " + film.getRegista());
        anno.setText("Anno: " + film.getAnno());
        durata.setText("Durata: " + film.getDurataMinuti() + " min");
        dataOra.setText("Inizio: " + p.getDataOra().format(FORMATO));
        LocalDateTime dataFine = p.getDataOraFine();
        fine.setText("Fine prevista: " + (dataFine != null ? dataFine.format(FORMATO) : "-"));
        prezzo.setText(String.format("Prezzo biglietto: %.2f \u20ac", p.getCostoBiglietto()));

        int postiLiberi = p.getPostiLiberi();
        posti.getStyleClass().remove("testo-esaurito");
        if (postiLiberi <= 0) {
            posti.setText("Esaurito");
            posti.getStyleClass().add("testo-esaurito");
        } else {
            posti.setText(postiLiberi + (postiLiberi == 1 ? " posto libero" : " posti liberi"));
        }

        configuraBottonePrenota(p, postiLiberi, isGuest, isProiezionista, dataNascita);

        setVisible(true);
        setManaged(true);
    }

    /*
     Decide stato ed eventuale avviso del bottone "Prenota", con la stessa logica usata
     dalle card: il proiezionista non prenota (niente bottone); il Guest è invitato ad
     accedere; chi non ha l'età minima o cerca una proiezione passata/esaurita trova il
     bottone bloccato con la motivazione.
     */
    private void configuraBottonePrenota(Proiezione p, int postiLiberi, boolean isGuest,
                                         boolean isProiezionista, LocalDate dataNascita) {
        avviso.setManaged(false);
        avviso.setVisible(false);
        btnPrenota.setDisable(false);
        btnPrenota.setManaged(true);
        btnPrenota.setVisible(true);
        Tooltip.install(btnPrenota, null);

        // Il proiezionista consulta i dettagli ma non prenota: niente bottone.
        if (!isGuest && isProiezionista) {
            btnPrenota.setManaged(false);
            btnPrenota.setVisible(false);
            return;
        }

        // Guest: bottone visibile ma bloccato, con invito ad accedere.
        if (isGuest) {
            btnPrenota.setDisable(true);
            mostraAvviso("Accedi come cliente per prenotare.");
            return;
        }

        // Proiezione già avvenuta.
        if (!p.getDataOra().isAfter(LocalDateTime.now())) {
            btnPrenota.setDisable(true);
            mostraAvviso("Proiezione gia' avvenuta: non prenotabile.");
            return;
        }

        // Esaurita.
        if (postiLiberi <= 0) {
            btnPrenota.setDisable(true);
            mostraAvviso("Proiezione esaurita.");
            return;
        }

        // Blocco per età minima.
        int etaMinima = p.getFilm().getEtaMinima();
        if (!FasciaEta.puoPrenotare(dataNascita, etaMinima)) {
            btnPrenota.setDisable(true);
            mostraAvviso("Vietato ai minori di " + etaMinima + " anni.");
        }
    }

    private void mostraAvviso(String testo) {
        avviso.setText(testo);
        avviso.setManaged(true);
        avviso.setVisible(true);
    }

    // Nasconde l'overlay e azzera lo stato corrente.
    public void chiudi() {
        setVisible(false);
        setManaged(false);
        proiezioneCorrente = null;
        onPrenota = null;
    }
}
