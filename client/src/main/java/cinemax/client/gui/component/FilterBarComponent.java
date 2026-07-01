package cinemax.client.gui.component;

import cinemax.common.model.CriteriRicercaProiezione;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Barra di ricerca riutilizzabile, costruita interamente in codice Java (niente FXML).
 * <p>
 * Design compatto: a riposo mostra solo il campo di ricerca per titolo, un bottone
 * "Filtri" e il bottone "Cerca". I filtri avanzati (genere, intervallo di date,
 * intervallo di prezzo) sono raccolti in un pannello che si apre/chiude cliccando
 * "Filtri", così la barra resta pulita ma mantiene tutte le funzionalità.
 * <p>
 * Un piccolo contatore sul bottone "Filtri" indica quanti filtri avanzati sono attivi,
 * utile quando il pannello è chiuso.
 * <p>
 * Alla pressione di "Cerca" i criteri vengono impacchettati in un oggetto
 * {@link CriteriRicercaProiezione} e passati al controller padre tramite il listener
 * registrato con {@link #setListenerRicerca(Consumer)}.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class FilterBarComponent extends VBox {

    private final TextField campoTitolo = new TextField();
    private final TextField campoGenere = new TextField();
    private final DatePicker dataDa = new DatePicker();
    private final DatePicker dataA = new DatePicker();
    private final TextField prezzoMin = new TextField();
    private final TextField prezzoMax = new TextField();

    private final Button btnCerca = new Button("Cerca");
    private final Button btnFiltri = new Button("Filtri");
    private final Label badgeFiltri = new Label();

    // Pannello dei filtri avanzati (genere, date, prezzo), mostrato/nascosto al toggle
    private final VBox pannelloAvanzati;

    // Colonne del pannello avanzati che possono essere nascoste in blocco
    private VBox colonnaDate1;
    private VBox colonnaDate2;
    private VBox colonnaPrezzo;

    // Etichetta di errore per input non validi (es. prezzo non numerico)
    private final Label labelErrore = new Label();

    // Listener verso il controller padre: riceve i criteri quando l'utente cerca
    private Consumer<CriteriRicercaProiezione> listenerRicerca;

    private boolean pannelloAperto = false;

    /** Costruisce la barra di ricerca con la riga principale sempre visibile e il
     *  pannello dei filtri avanzati inizialmente chiuso. */
    public FilterBarComponent() {
        super(0);
        getStyleClass().add("barra-filtri");

        pannelloAvanzati = costruisciPannelloAvanzati();
        pannelloAvanzati.setVisible(false);
        pannelloAvanzati.setManaged(false);

        HBox rigaPrincipale = costruisciRigaPrincipale();

        labelErrore.getStyleClass().add("campo-errore");
        labelErrore.setPadding(new Insets(6, 0, 0, 4));
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);

        getChildren().addAll(rigaPrincipale, pannelloAvanzati, labelErrore);

        aggiornaBadgeFiltri();
    }

    /**
     * Costruisce la riga sempre visibile: campo di ricerca, toggle "Filtri" e "Cerca".
     *
     * @return la riga principale della barra
     */
    private HBox costruisciRigaPrincipale() {
        HBox riga = new HBox(10);
        riga.setAlignment(Pos.CENTER_LEFT);
        riga.setPadding(new Insets(12));

        // Box di ricerca con "icona" lente (glifo) + campo titolo, a tutta larghezza.
        HBox boxRicerca = new HBox(8);
        boxRicerca.setAlignment(Pos.CENTER_LEFT);
        boxRicerca.getStyleClass().add("box-ricerca");
        HBox.setHgrow(boxRicerca, Priority.ALWAYS);

        Label lente = new Label("🔍");
        lente.getStyleClass().add("icona-ricerca");

        campoTitolo.setPromptText("Cerca un film per titolo...");
        campoTitolo.getStyleClass().add("campo-ricerca-interno");
        HBox.setHgrow(campoTitolo, Priority.ALWAYS);
        // Invio nel campo titolo = avvio ricerca, comodità d'uso.
        campoTitolo.setOnAction(e -> onCercaCliccato());

        boxRicerca.getChildren().addAll(lente, campoTitolo);

        badgeFiltri.getStyleClass().add("badge-filtri");
        badgeFiltri.setManaged(false);
        badgeFiltri.setVisible(false);
        btnFiltri.setText("Filtri");
        btnFiltri.setGraphic(badgeFiltri);
        btnFiltri.setContentDisplay(ContentDisplay.RIGHT);
        btnFiltri.setGraphicTextGap(6);
        btnFiltri.getStyleClass().add("bottone-secondario");
        btnFiltri.setOnAction(e -> togglePannello());

        btnCerca.getStyleClass().add("bottone-primario");
        btnCerca.setOnAction(e -> onCercaCliccato());

        riga.getChildren().addAll(boxRicerca, btnFiltri, btnCerca);
        return riga;
    }

    /**
     * Costruisce il pannello dei filtri avanzati: genere, date (da/a), prezzo (min/max)
     * e il bottone "Azzera filtri".
     *
     * @return il contenitore del pannello avanzati
     */
    private VBox costruisciPannelloAvanzati() {
        GridPane griglia = new GridPane();
        griglia.getStyleClass().add("pannello-filtri");
        griglia.setHgap(12);
        griglia.setVgap(10);
        griglia.setPadding(new Insets(0, 12, 0, 12));

        VBox colonnaGenere = costruisciColonna("Genere", campoGenere);
        campoGenere.setPromptText("Tutti");

        dataDa.setPromptText("gg/mm/aaaa");
        dataA.setPromptText("gg/mm/aaaa");
        colonnaDate1 = costruisciColonna("Dal", dataDa);
        colonnaDate2 = costruisciColonna("Al", dataA);

        // Colonna prezzo: due campi min/max affiancati sotto un'unica etichetta.
        prezzoMin.setPromptText("min");
        prezzoMin.getStyleClass().add("campo-testo");
        prezzoMax.setPromptText("max");
        prezzoMax.getStyleClass().add("campo-testo");
        HBox rigaPrezzo = new HBox(6);
        rigaPrezzo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(prezzoMin, Priority.ALWAYS);
        HBox.setHgrow(prezzoMax, Priority.ALWAYS);
        prezzoMin.setMaxWidth(Double.MAX_VALUE);
        prezzoMax.setMaxWidth(Double.MAX_VALUE);
        Label trattino = new Label("-");
        trattino.getStyleClass().add("etichetta-campo");
        rigaPrezzo.getChildren().addAll(prezzoMin, trattino, prezzoMax);
        colonnaPrezzo = costruisciColonna("Prezzo (€)", rigaPrezzo);

        griglia.add(colonnaGenere, 0, 0);
        griglia.add(colonnaDate1, 1, 0);
        griglia.add(colonnaDate2, 2, 0);
        griglia.add(colonnaPrezzo, 3, 0);

        // Quattro colonne di pari larghezza, responsive.
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setHgrow(Priority.ALWAYS);
            griglia.getColumnConstraints().add(cc);
        }

        // Riga col bottone "Azzera filtri": cancella i filtri avanzati ma tiene il titolo.
        Button btnAzzera = new Button("Azzera filtri");
        btnAzzera.getStyleClass().add("bottone-secondario");
        btnAzzera.setOnAction(e -> azzeraFiltriTranneTitolo());
        HBox rigaAzzera = new HBox(btnAzzera);
        rigaAzzera.setAlignment(Pos.CENTER_RIGHT);
        rigaAzzera.setPadding(new Insets(10, 12, 12, 12));

        VBox contenitore = new VBox(griglia, rigaAzzera);
        return contenitore;
    }

    /**
     * Costruisce una colonna "etichetta sopra, controllo sotto" a tutta larghezza.
     *
     * @param etichetta testo dell'etichetta
     * @param controllo nodo di input della colonna
     * @return la colonna (VBox) etichetta + controllo
     */
    private VBox costruisciColonna(String etichetta, javafx.scene.Node controllo) {
        VBox colonna = new VBox(4);
        Label label = new Label(etichetta);
        label.getStyleClass().add("etichetta-campo");

        if (controllo instanceof javafx.scene.control.Control) {
            ((javafx.scene.control.Control) controllo).setMaxWidth(Double.MAX_VALUE);
            if (!controllo.getStyleClass().contains("campo-testo")) {
                controllo.getStyleClass().add("campo-testo");
            }
        }
        colonna.getChildren().addAll(label, controllo);
        return colonna;
    }

    /** Apre o chiude il pannello dei filtri avanzati, aggiornando lo stile del bottone. */
    private void togglePannello() {
        pannelloAperto = !pannelloAperto;
        pannelloAvanzati.setVisible(pannelloAperto);
        pannelloAvanzati.setManaged(pannelloAperto);
        if (pannelloAperto) {
            btnFiltri.getStyleClass().add("bottone-attivo");
        } else {
            btnFiltri.getStyleClass().remove("bottone-attivo");
        }
    }

    /**
     * Mostra o nasconde i blocchi opzionali (date e prezzo) all'interno del pannello.
     * Se nessuno dei due è visibile, restano comunque accessibili titolo e genere.
     *
     * @param mostraDate   se true mostra le colonne dell'intervallo date
     * @param mostraPrezzo se true mostra la colonna dell'intervallo prezzo
     */
    public void impostaVisibilitaFiltri(boolean mostraDate, boolean mostraPrezzo) {
        colonnaDate1.setVisible(mostraDate);
        colonnaDate1.setManaged(mostraDate);
        colonnaDate2.setVisible(mostraDate);
        colonnaDate2.setManaged(mostraDate);
        colonnaPrezzo.setVisible(mostraPrezzo);
        colonnaPrezzo.setManaged(mostraPrezzo);
    }

    /**
     * Registra il listener che riceverà i criteri quando l'utente preme Cerca.
     *
     * @param listener callback che riceve i criteri di ricerca compilati
     */
    public void setListenerRicerca(Consumer<CriteriRicercaProiezione> listener) {
        this.listenerRicerca = listener;
    }

    /**
     * Costruisce i {@link CriteriRicercaProiezione} dai campi e li passa al listener del
     * padre. Valida localmente i prezzi (devono essere numerici) mostrando un errore in
     * caso contrario.
     */
    private void onCercaCliccato() {
        pulisciErrore();

        Double min = leggiPrezzo(prezzoMin.getText());
        Double max = leggiPrezzo(prezzoMax.getText());

        // Validazione minima locale: i prezzi, se presenti, devono essere numeri.
        if (prezzoNonValido(prezzoMin.getText(), min) || prezzoNonValido(prezzoMax.getText(), max)) {
            mostraErrore("Il prezzo deve essere un numero (es. 9.50).");
            if (!pannelloAperto) {
                togglePannello(); // apro i filtri così l'utente vede dov'è l'errore
            }
            return;
        }

        CriteriRicercaProiezione criteri = new CriteriRicercaProiezione(
                vuotoComeNull(campoTitolo.getText()),
                vuotoComeNull(campoGenere.getText()),
                dataDa.getValue(),   // dataInizio, può essere null
                dataA.getValue(),    // dataFine, può essere null
                min,                 // costoMin, può essere null
                max                  // costoMax, può essere null
        );

        aggiornaBadgeFiltri();

        if (listenerRicerca != null) {
            listenerRicerca.accept(criteri);
        }
    }

    /** Reimposta tutti i campi (titolo e filtri avanzati) allo stato iniziale vuoto. */
    public void svuotaFiltri() {
        campoTitolo.clear();
        campoGenere.clear();
        dataDa.setValue(null);
        dataA.setValue(null);
        prezzoMin.clear();
        prezzoMax.clear();
        pulisciErrore();
        aggiornaBadgeFiltri();
    }

    /**
     * Azzera i filtri avanzati (genere, date, prezzo) mantenendo il titolo inserito, poi
     * rilancia la ricerca con il solo titolo rimasto.
     */
    public void azzeraFiltriTranneTitolo() {
        campoGenere.clear();
        dataDa.setValue(null);
        dataA.setValue(null);
        prezzoMin.clear();
        prezzoMax.clear();
        pulisciErrore();
        aggiornaBadgeFiltri();
        onCercaCliccato(); // ripropone i risultati col solo titolo
    }

    /**
     * Imposta dei valori predefiniti nei filtri (es. titolo passato dall'utente Guest nel
     * menu iniziale). Passare null a un parametro lo lascia invariato.
     *
     * @param titolo titolo da precompilare (null = invariato)
     * @param genere genere da precompilare (null = invariato)
     */
    public void impostaFiltriPredefiniti(String titolo, String genere) {
        if (titolo != null) {
            campoTitolo.setText(titolo);
        }
        if (genere != null) {
            campoGenere.setText(genere);
        }
        aggiornaBadgeFiltri();
    }

    /** Conta i filtri avanzati attivi (genere, date, prezzo) e aggiorna il badge numerico. */
    private void aggiornaBadgeFiltri() {
        int attivi = 0;
        if (vuotoComeNull(campoGenere.getText()) != null) attivi++;
        if (dataDa.getValue() != null) attivi++;
        if (dataA.getValue() != null) attivi++;
        if (vuotoComeNull(prezzoMin.getText()) != null) attivi++;
        if (vuotoComeNull(prezzoMax.getText()) != null) attivi++;

        boolean ce = attivi > 0;
        badgeFiltri.setText(String.valueOf(attivi));
        badgeFiltri.setManaged(ce);
        badgeFiltri.setVisible(ce);
    }

    // Helper privati

    /**
     * @param s stringa da normalizzare
     * @return la stringa senza spazi ai lati, oppure null se vuota o composta di soli spazi
     */
    private static String vuotoComeNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /**
     * Converte la stringa in Double (accettando la virgola come separatore decimale).
     *
     * @param s testo da convertire
     * @return il valore numerico, oppure null se la stringa è vuota o non numerica
     */
    private static Double leggiPrezzo(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @param testo  testo inserito dall'utente
     * @param valore valore numerico già convertito (null se conversione fallita)
     * @return true se l'utente ha scritto qualcosa che non è un numero valido
     */
    private static boolean prezzoNonValido(String testo, Double valore) {
        boolean testoPresente = testo != null && !testo.trim().isEmpty();
        return testoPresente && valore == null;
    }

    /**
     * Mostra un messaggio di errore sotto la barra.
     *
     * @param messaggio testo dell'errore da mostrare
     */
    private void mostraErrore(String messaggio) {
        labelErrore.setText(messaggio);
        labelErrore.setManaged(true);
        labelErrore.setVisible(true);
    }

    /** Nasconde e svuota il messaggio di errore. */
    private void pulisciErrore() {
        labelErrore.setText("");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
    }
}
