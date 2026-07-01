package cinemax.client.gui.component.card;

import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.Film;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Scheda (card) che mostra i dati di una singola Prenotazione, costruita interamente in
 * codice Java (niente FXML, come il resto della UI).
 * <p>
 * La card ha due modalità, decise dal controller:
 * <ul>
 *   <li>ATTIVA (proiezione futura): mostra i bottoni "Modifica" e "Annulla";</li>
 *   <li>PASSATA (proiezione già avvenuta): card semi-trasparente, sola consultazione,
 *       nessuna azione (una prenotazione passata è già stata fruita).</li>
 * </ul>
 * Il codice della prenotazione è mostrato in un campo di sola lettura ma selezionabile,
 * così è comodo da copiare/incollare.
 * <p>
 * Le azioni vengono iniettate dal controller con {@link #setAzioneAnnulla},
 * {@link #setAzioneModifica} e {@link #setAzioneCard}: la card non conosce il server, si
 * limita a raccogliere il click e a richiamare l'azione.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class CardPrenotazione extends VBox {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final Label titoloLabel = new Label();
    private final Circle pallinoEta = new Circle(6);
    private final Label etaLabel = new Label();
    private final TextField codiceCampo = new TextField();
    private final Label dataLabel = new Label();
    private final Label bigliettiLabel = new Label();

    // Riga azioni (vuota per le card passate).
    private final HBox rigaAzioni = new HBox(10);

    private Prenotazione prenotazioneCorrente;

    /** Costruisce una card prenotazione vuota; la riga azioni resta vuota finché il
     *  controller non registra le azioni (card attive) e assente per le passate. */
    public CardPrenotazione() {
        super(2);
        setPadding(new Insets(8, 16, 8, 16));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("card-proiezione");

        titoloLabel.getStyleClass().add("card-titolo");
        titoloLabel.setWrapText(true);

        etaLabel.getStyleClass().add("testo-secondario");

        // Titolo + pallino età + valore numerico, sulla stessa riga (come la card proiezione).
        Region spazioTitolo = new Region();
        HBox.setHgrow(spazioTitolo, Priority.ALWAYS);
        HBox rigaTitolo = new HBox(8, titoloLabel, spazioTitolo, pallinoEta, etaLabel);
        rigaTitolo.setAlignment(Pos.CENTER_LEFT);

        // Codice in un campo di sola lettura ma selezionabile (comodo per il copia-incolla).
        codiceCampo.setEditable(false);
        codiceCampo.setFocusTraversable(false);
        codiceCampo.getStyleClass().add("campo-codice");

        dataLabel.getStyleClass().add("testo-normale");
        bigliettiLabel.getStyleClass().add("testo-normale");

        Label etichettaCodice = new Label("Codice:");
        etichettaCodice.getStyleClass().add("testo-secondario");
        HBox rigaCodice = new HBox(8, etichettaCodice, codiceCampo);
        rigaCodice.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(codiceCampo, Priority.ALWAYS);

        HBox rigaInfo = new HBox(20, dataLabel, bigliettiLabel);
        rigaInfo.setAlignment(Pos.CENTER_LEFT);

        rigaAzioni.setAlignment(Pos.CENTER_RIGHT);

        // Interlinea fra la riga delle informazioni e la riga dei bottoni, coerente con
        // la card proiezione: le azioni non risultano appiccicate ai dati.
        Region staccoAzioni = new Region();
        staccoAzioni.setMinHeight(8);

        getChildren().addAll(rigaTitolo, rigaCodice, rigaInfo, staccoAzioni, rigaAzioni);
    }

    /**
     * Popola la card con i dati di una prenotazione. Il parametro passata decide la
     * modalità: true = prenotazione passata (semi-trasparente, sola consultazione),
     * false = attiva (modificabile/annullabile).
     *
     * @param p       prenotazione da mostrare
     * @param passata true se la proiezione è già avvenuta (card in sola lettura)
     */
    public void compilaDatiPrenotazione(Prenotazione p, boolean passata) {
        this.prenotazioneCorrente = p;

        Proiezione proiezione = p.getProiezione();
        Film film = proiezione.getFilm();

        titoloLabel.setText(film.getTitolo());
        impostaPallinoEta(film.getEtaMinima());
        codiceCampo.setText(p.getCodice());
        dataLabel.setText(proiezione.getDataOra().format(FORMATO_DATA));

        int n = p.getNumeroBiglietti();
        double totale = n * proiezione.getCostoBiglietto();
        bigliettiLabel.setText(n + (n == 1 ? " biglietto" : " biglietti")
                + String.format("  -  totale %.2f €", totale));

        // Le card passate sono attenuate e senza azioni.
        getStyleClass().remove("card-passata");
        if (passata) {
            getStyleClass().add("card-passata");
        }
    }

    /**
     * Registra l'azione di annullamento (solo per le card attive). Registrare l'azione
     * mostra il bottone "Annulla prenotazione".
     *
     * @param azione callback invocata al click, riceve la prenotazione della card
     */
    public void setAzioneAnnulla(Consumer<Prenotazione> azione) {
        Button btnAnnulla = new Button("Annulla prenotazione");
        btnAnnulla.getStyleClass().add("bottone-secondario");
        // Il click sul bottone non deve propagarsi alla card (che apre i dettagli).
        btnAnnulla.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                javafx.event.Event::consume);
        btnAnnulla.setOnAction(e -> {
            if (azione != null && prenotazioneCorrente != null) {
                azione.accept(prenotazioneCorrente);
            }
        });
        rigaAzioni.getChildren().add(btnAnnulla);
    }

    /**
     * Registra l'azione di modifica/spostamento (solo per le card attive). Mostra il
     * bottone "Modifica prenotazione", inserito prima di "Annulla" nella riga azioni.
     *
     * @param azione callback invocata al click, riceve la prenotazione della card
     */
    public void setAzioneModifica(Consumer<Prenotazione> azione) {
        Button btnModifica = new Button("Mod. prenotazione");
        btnModifica.getStyleClass().add("bottone-primario");
        btnModifica.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                javafx.event.Event::consume);
        btnModifica.setOnAction(e -> {
            if (azione != null && prenotazioneCorrente != null) {
                azione.accept(prenotazioneCorrente);
            }
        });
        // Inserisco in testa, cosi' l'ordine è "Modifica" poi "Annulla".
        rigaAzioni.getChildren().add(0, btnModifica);
    }

    /**
     * Registra l'azione associata all'intera card (apertura dei dettagli prenotazione).
     * Rende la card cliccabile col mouse e attivabile da tastiera (INVIO/SPAZIO). Usata
     * solo per le card attive: le passate restano in sola consultazione.
     *
     * @param azione callback invocata all'attivazione della card, riceve la prenotazione
     */
    public void setAzioneCard(Consumer<Prenotazione> azione) {
        setFocusTraversable(true);
        setOnMouseClicked(e -> {
            if (azione != null && prenotazioneCorrente != null) {
                azione.accept(prenotazioneCorrente);
            }
        });
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> {
                    if (azione != null && prenotazioneCorrente != null) {
                        azione.accept(prenotazioneCorrente);
                        e.consume();
                    }
                }
                default -> { }
            }
        });
    }

    /**
     * Colora il pallino in base alla fascia d'età e mostra il valore (es. "VM13"),
     * installando come tooltip la descrizione del limite d'età.
     *
     * @param etaMinima età minima richiesta dal film
     */
    private void impostaPallinoEta(int etaMinima) {
        FasciaEta.Fascia fascia = FasciaEta.fasciaPerEta(etaMinima);
        pallinoEta.getStyleClass().setAll("pallino-eta", fascia.getClasseCss());
        etaLabel.setText("VM" + etaMinima);
        String testoTooltip = etaMinima <= 0
                ? "Adatto a tutte le età!"
                : "Vietato ai minori di " + etaMinima + " anni";
        Tooltip.install(pallinoEta, new Tooltip(testoTooltip));
    }

    /** @return la prenotazione attualmente mostrata dalla card. */
    public Prenotazione getPrenotazione() {
        return prenotazioneCorrente;
    }
}
