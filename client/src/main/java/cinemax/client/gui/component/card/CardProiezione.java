package cinemax.client.gui.component.card;

import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Scheda (card) che mostra i dati di una singola Proiezione, costruita interamente in
 * codice Java (niente FXML, come il resto della UI).
 * <p>
 * Layout compatto: titolo (con pallino del limite d'età accanto), una riga di dettagli
 * film, una riga con data/prezzo/posti e la riga azioni.
 * <p>
 * Il pallino d'età usa un semaforo (verde/giallo/arancione/rosso) calcolato da
 * {@link FasciaEta} in base all'età minima del film. Accanto al pallino è mostrato il
 * valore numerico (es. "VM13").
 * <p>
 * Riutilizzo tra ruoli: il bottone azione principale cambia etichetta e comportamento a
 * seconda del ruolo (es. "Prenota" per il cliente, "Modifica" per il proiezionista). Le
 * azioni vengono iniettate dal controller padre tramite {@link #setAzionePrincipale} e
 * {@link #setAzioneSecondaria}, così la stessa card serve dashboard diverse.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class CardProiezione extends VBox {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final Label titoloLabel = new Label();
    private final Circle pallinoEta = new Circle(6);
    private final Label etaLabel = new Label();
    private final Label dettagliFilmLabel = new Label();
    private final Label dataLabel = new Label();
    private final Label prezzoLabel = new Label();
    private final Label postiLabel = new Label();

    private final Button bottonePrincipale = new Button();
    private final Button bottoneSecondario = new Button();

    // Proiezione attualmente mostrata, passata alle azioni come argomento.
    private Proiezione proiezioneCorrente;

    /** Costruisce una card proiezione vuota, focusabile da tastiera; i bottoni azione
     *  restano nascosti finché non viene registrata la relativa azione. */
    public CardProiezione() {
        super(2);
        setPadding(new Insets(8, 16, 8, 16));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("card-proiezione");
        // La card è raggiungibile con TAB e attivabile da tastiera (vedi setAzioneCard):
        // serve sia per l'accessibilita' sia per far scattare lo stile :focused del tema.
        setFocusTraversable(true);

        titoloLabel.getStyleClass().add("card-titolo");
        titoloLabel.setWrapText(true);

        etaLabel.getStyleClass().add("testo-secondario");

        // Titolo + pallino età + valore numerico, sulla stessa riga.
        Region spazioTitolo = new Region();
        HBox.setHgrow(spazioTitolo, Priority.ALWAYS);
        HBox rigaTitolo = new HBox(8, titoloLabel, spazioTitolo, pallinoEta, etaLabel);
        rigaTitolo.setAlignment(Pos.CENTER_LEFT);

        dettagliFilmLabel.getStyleClass().add("testo-secondario");
        dettagliFilmLabel.setWrapText(true);

        dataLabel.getStyleClass().add("testo-normale");
        prezzoLabel.getStyleClass().add("testo-normale");
        postiLabel.getStyleClass().add("testo-normale");

        bottonePrincipale.getStyleClass().add("bottone-primario");
        bottoneSecondario.getStyleClass().add("bottone-secondario");

        // I bottoni sono nascosti finchè non viene registrata un'azione per loro.
        bottonePrincipale.setManaged(false);
        bottonePrincipale.setVisible(false);
        bottoneSecondario.setManaged(false);
        bottoneSecondario.setVisible(false);

        HBox rigaInfo = new HBox(16, dataLabel, prezzoLabel, postiLabel);
        rigaInfo.setAlignment(Pos.CENTER_LEFT);

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);
        HBox rigaAzioni = new HBox(8, spazio, bottonePrincipale, bottoneSecondario);
        rigaAzioni.setAlignment(Pos.CENTER_RIGHT);

        Region staccoAzioni = new Region();
        staccoAzioni.setMinHeight(16);

        getChildren().addAll(rigaTitolo, dettagliFilmLabel, rigaInfo, staccoAzioni, rigaAzioni);
    }

    /**
     * Popola la card con i dati di una proiezione. Il parametro ruoloUtente serve a
     * personalizzare l'etichetta del bottone principale (es. "Prenota" vs "Modifica").
     * Passare ruoloUtente null va bene per il Guest: in quel caso si usa l'etichetta di
     * default e il controllo accessi (attenuazione/blocco) è gestito dal layout tramite
     * {@code registraNodoRiservato(card.getBottonePrincipale())}.
     *
     * @param p           proiezione da mostrare
     * @param ruoloUtente ruolo dell'utente corrente (può essere null per il Guest)
     */
    public void compilaDatiProiezione(Proiezione p, Ruolo ruoloUtente) {
        this.proiezioneCorrente = p;

        Film film = p.getFilm();

        titoloLabel.setText(film.getTitolo());

        // Pallino del limite d'età (semaforo) + valore numerico.
        impostaPallinoEta(film.getEtaMinima());

        dettagliFilmLabel.setText(
                film.getGenere()
                        + "  -  " + film.getRegista()
                        + "  -  " + film.getAnno()
                        + "  -  " + film.getDurataMinuti() + " min"
        );

        dataLabel.setText(p.getDataOra().format(FORMATO_DATA));
        prezzoLabel.setText(String.format("%.2f €", p.getCostoBiglietto()));

        int posti = p.getPostiLiberi();
        postiLabel.getStyleClass().remove("testo-esaurito");
        if (posti <= 0) {
            postiLabel.setText("Esaurito");
            postiLabel.getStyleClass().add("testo-esaurito");
        } else {
            postiLabel.setText(posti + (posti == 1 ? " posto libero" : " posti liberi"));
        }

        // Etichetta del bottone principale in base al ruolo.
        if (ruoloUtente == Ruolo.PROIEZIONISTA) {
            bottonePrincipale.setText("Modifica");
        } else {
            bottonePrincipale.setText("Prenota");
        }
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

    /**
     * Blocca l'azione di prenotazione: disabilita il bottone principale e mostra il
     * motivo come tooltip. Usato quando l'utente non ha l'età minima per il film.
     *
     * @param motivo testo esplicativo del blocco, mostrato come tooltip
     */
    public void bloccaPrenotazione(String motivo) {
        bottonePrincipale.setDisable(true);
        Tooltip.install(bottonePrincipale, new Tooltip(motivo));
    }

    /**
     * Registra l'azione del bottone principale (es. "Prenota" per il cliente). Il
     * Consumer riceve la Proiezione mostrata da questa card. Registrare un'azione rende
     * il bottone visibile.
     *
     * @param azione callback invocata al click, riceve la proiezione della card
     */
    public void setAzionePrincipale(Consumer<Proiezione> azione) {
        bottonePrincipale.setManaged(true);
        bottonePrincipale.setVisible(true);
        // Consuma il click del mouse cosi' da non farlo risalire alla card: premendo questo
        // bottone (es. "Prenota") non deve scattare anche l'apertura dei dettagli, agganciata
        // al click sull'intera card nel controller padre.
        bottonePrincipale.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                javafx.event.Event::consume);
        bottonePrincipale.setOnAction(e -> {
            if (azione != null && proiezioneCorrente != null) {
                azione.accept(proiezioneCorrente);
            }
        });
    }

    /**
     * Registra l'azione del bottone secondario (es. "Elimina" per il proiezionista).
     * Registrare un'azione rende il bottone visibile.
     *
     * @param azione callback invocata al click, riceve la proiezione della card
     */
    public void setAzioneSecondaria(Consumer<Proiezione> azione) {
        bottoneSecondario.setManaged(true);
        bottoneSecondario.setVisible(true);
        bottoneSecondario.setText("Elimina");
        // Come per il bottone principale: il click non deve propagarsi alla card.
        bottoneSecondario.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                javafx.event.Event::consume);
        bottoneSecondario.setOnAction(e -> {
            if (azione != null && proiezioneCorrente != null) {
                azione.accept(proiezioneCorrente);
            }
        });
    }

    /**
     * Personalizza l'etichetta del bottone principale.
     *
     * @param testo nuovo testo del bottone principale
     */
    public void setEtichettaPrincipale(String testo) {
        bottonePrincipale.setText(testo);
    }

    /**
     * Espone il bottone principale per consentire al BaseLayoutController di registrarlo
     * come "nodo riservato": per il Guest verrà attenuato e reso non cliccabile.
     *
     * @return il bottone azione principale della card
     */
    public Button getBottonePrincipale() {
        return bottonePrincipale;
    }

    /** @return il bottone azione secondario della card. */
    public Button getBottoneSecondario() {
        return bottoneSecondario;
    }

    /** @return la proiezione attualmente mostrata dalla card. */
    public Proiezione getProiezione() {
        return proiezioneCorrente;
    }

    /**
     * Registra l'azione associata all'intera card (apertura dei dettagli). Oltre al click
     * del mouse, la collega a INVIO e SPAZIO quando la card ha il focus da tastiera, così
     * l'interazione è completa anche senza mouse.
     *
     * @param azione callback invocata all'attivazione della card, riceve la proiezione
     */
    public void setAzioneCard(Consumer<Proiezione> azione) {
        setOnMouseClicked(e -> {
            if (azione != null && proiezioneCorrente != null) {
                azione.accept(proiezioneCorrente);
            }
        });
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> {
                    if (azione != null && proiezioneCorrente != null) {
                        azione.accept(proiezioneCorrente);
                        e.consume();
                    }
                }
                default -> { }
            }
        });
    }
}
