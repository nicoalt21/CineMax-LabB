package cinemax.client.gui.component;

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

/*
 Scheda (card) che mostra i dati di una singola Prenotazione, costruita interamente in
 codice Java (niente FXML, come il resto della UI).

 La card ha due modalità, decise dal controller:
   - ATTIVA (proiezione futura): mostra il bottone "Annulla prenotazione".
   - PASSATA (proiezione precedente a oggi): card semi-trasparente, sola consultazione,
     nessuna azione (una prenotazione passata è già stata fruita).

 Il codice della prenotazione è mostrato in un campo di sola lettura ma selezionabile,
 così è comodo da copiare/incollare.

 L'azione di annullamento viene iniettata dal controller con setAzioneAnnulla(...): la
 card non sa nulla del server, si limita a raccogliere il click e a richiamare l'azione.
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

        getChildren().addAll(rigaTitolo, rigaCodice, rigaInfo, rigaAzioni);
    }

    /*
     Popola la card con i dati di una prenotazione. Il parametro passata decide la
     modalità: true = prenotazione passata (semi-trasparente, sola consultazione),
     false = attiva (annullabile).
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

    /*
     Registra l'azione di annullamento (solo per le card attive). Il Consumer riceve la
     prenotazione. Registrare l'azione mostra il bottone "Annulla prenotazione".
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

    /*
     Registra l'azione di modifica/spostamento (solo per le card attive). Mostra il
     bottone "Modifica prenotazione". Per coerenza visiva il bottone modifica appare
     prima di "Annulla" nella riga azioni.
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
        // Inserisco in testa, cosi' l'ordine e' "Modifica" poi "Annulla".
        rigaAzioni.getChildren().add(0, btnModifica);
    }

    /*
     Registra l'azione associata all'intera card (apertura dei dettagli prenotazione).
     Rende la card cliccabile col mouse e attivabile da tastiera (INVIO/SPAZIO). Usata
     solo per le card attive: le passate restano in sola consultazione.
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

    // Colora il pallino in base alla fascia d'età e mostra il valore (es. "VM13").
    private void impostaPallinoEta(int etaMinima) {
        FasciaEta.Fascia fascia = FasciaEta.fasciaPerEta(etaMinima);
        pallinoEta.getStyleClass().setAll("pallino-eta", fascia.getClasseCss());
        etaLabel.setText("VM" + etaMinima);
        Tooltip.install(pallinoEta, new Tooltip("Vietato ai minori di " + etaMinima + " anni"));
    }

    public Prenotazione getPrenotazione() {
        return prenotazioneCorrente;
    }
}
