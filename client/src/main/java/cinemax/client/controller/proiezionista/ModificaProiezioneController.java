/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.controller.proiezionista;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CampoConEtichetta;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 Schermata "Modifica proiezione" del proiezionista, gemella di CreaProiezioneController ma
 con i campi precompilati dai valori attuali. Il calcolo delle finestre libere esclude la
 proiezione in modifica (cosi il suo slot resta selezionabile) e la conferma invoca
 ServizioProiezioni.modificaProiezione(dataOraAttuale, idFilm, nuovaDataOra, costo).

 Vincoli (verificati dal server, anticipati qui dove possibile): una proiezione con
 prenotazioni non e modificabile; la nuova collocazione non deve sovrapporsi ad altre.
 */
public class ModificaProiezioneController extends DashboardBaseController {

    private static final double LARGHEZZA_CAMPO = 260;
    private static final double LARGHEZZA_GRIGLIA = LARGHEZZA_CAMPO * 2 + 24;
    private static final DateTimeFormatter FORMATO_ORA = DateTimeFormatter.ofPattern("HH:mm");

    // Capienza della sala (cinema monosala, come da specifiche). Serve solo per dedurre,
    // dai posti liberi, se la proiezione ha già prenotazioni e avvisare l'utente.
    private static final int CAPIENZA_SALA = 200;

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    // Proiezione da modificare. La sua data_ora attuale è l'identificatore lato server e
    // va passata sia a finestreLibere (per escluderla) sia a modificaProiezione.
    private final Proiezione proiezioneOriginale;
    private final LocalDateTime dataOraAttuale;

    private final VBox radice = new VBox(15);

    private final ComboBox<Film> selettoreTitolo = new ComboBox<>();
    private final CampoConEtichetta campoTitolo =
            new CampoConEtichetta("Titolo film", true, selettoreTitolo, LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoCosto =
            new CampoConEtichetta("Costo biglietto (\u20ac)", true, nuovoTextField("Es. 9.50"), LARGHEZZA_CAMPO);
    private final CampoConEtichetta campoData =
            new CampoConEtichetta("Data", true, new DatePicker(), LARGHEZZA_CAMPO);
    private final ComboBox<LocalTime> selettoreOra = new ComboBox<>();
    private final CampoConEtichetta campoOra =
            new CampoConEtichetta("Orario (finestre libere)", true, selettoreOra, LARGHEZZA_CAMPO);

    private final Label labelMessaggio = new Label();

    // Film canonico corrispondente al titolo digitato/scelto (null finché non è valido).
    private Film filmRiconosciuto = null;
    private boolean aggiornamentoInterno = false;

    public ModificaProiezioneController(GestoreScene gestoreScene,
                                        BaseLayoutController layout,
                                        Proiezione proiezione) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
        this.proiezioneOriginale = proiezione;
        this.dataOraAttuale = proiezione.getDataOra();
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

        Label titolo = new Label("Modifica proiezione");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        configuraSelettoreTitolo();
        configuraCampoCosto();
        configuraSelettoreOra();
        configuraDatePicker();

        VBox modulo = new VBox(12);
        modulo.setAlignment(Pos.CENTER);
        modulo.setMaxWidth(LARGHEZZA_GRIGLIA);

        labelMessaggio.getStyleClass().add("errore-generale");
        labelMessaggio.setWrapText(true);
        labelMessaggio.setMaxWidth(LARGHEZZA_GRIGLIA);
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);

        Button btnSalva = new Button("Salva modifiche");
        btnSalva.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnSalva.getStyleClass().add("bottone-primario");
        btnSalva.setOnAction(e -> salva());

        Button btnIndietro = new Button("Annulla");
        btnIndietro.setMaxWidth(LARGHEZZA_GRIGLIA);
        btnIndietro.getStyleClass().add("bottone-secondario");
        btnIndietro.setOnAction(e -> layout.mostraDashboardRicercaPubblica());

        modulo.getChildren().addAll(
                costruisciGriglia(),
                labelMessaggio,
                btnSalva,
                btnIndietro
        );

        ScrollPane scroll = new ScrollPane(modulo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titolo, scroll);

        // Avviso preventivo: se la proiezione ha già prenotazioni, il server rifiuterà la
        // modifica. Lo segnaliamo subito (i posti liberi < capienza implicano prenotazioni).
        avvisaSePrenotata();

        precompilaConValoriAttuali();
    }

    @Override
    public void aggiornaDati() {
        precompilaConValoriAttuali();
    }

    // Riempie i campi con i dati attuali della proiezione e calcola le finestre disponibili.
    private void precompilaConValoriAttuali() {
        aggiornamentoInterno = true;

        // Titolo / film corrente.
        filmRiconosciuto = proiezioneOriginale.getFilm();
        caricaSuggerimenti();
        selettoreTitolo.getEditor().setText(filmRiconosciuto.getTitolo());

        // Costo corrente.
        ((TextField) campoCosto.getControllo())
                .setText(String.format("%.2f", proiezioneOriginale.getCostoBiglietto()).replace(',', '.'));

        // Data corrente.
        ((DatePicker) campoData.getControllo()).setValue(dataOraAttuale.toLocalDate());

        aggiornamentoInterno = false;

        pulisciErroriCampi();
        // Calcola le finestre per la data corrente, preselezionando l'orario attuale.
        aggiornaFinestreLibere(dataOraAttuale.toLocalTime());
    }

    private void configuraSelettoreTitolo() {
        selettoreTitolo.setEditable(true);
        selettoreTitolo.setMaxWidth(Double.MAX_VALUE);
        selettoreTitolo.setVisibleRowCount(6);
        selettoreTitolo.getEditor().setPromptText("Es. Inception");

        selettoreTitolo.setConverter(new StringConverter<Film>() {
            @Override
            public String toString(Film f) {
                return f == null ? "" : f.getTitolo();
            }

            @Override
            public Film fromString(String s) {
                // La ComboBox al commit di focus ricava il value da qui: ritornare null
                // azzererebbe value ed editor. Risolvo quindi il testo nel Film con titolo
                // corrispondente (case-insensitive) tra gli item caricati.
                if (s == null || s.isBlank() || selettoreTitolo.getItems() == null) {
                    return null;
                }
                String norm = normalizza(s);
                for (Film f : selettoreTitolo.getItems()) {
                    if (normalizza(f.getTitolo()).equals(norm)) {
                        return f;
                    }
                }
                return null;
            }
        });
        selettoreTitolo.setCellFactory(lv -> new javafx.scene.control.ListCell<Film>() {
            @Override
            protected void updateItem(Film f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? null : f.getTitolo() + "  (" + f.getAnno() + ")");
            }
        });

        selettoreTitolo.getSelectionModel().selectedItemProperty().addListener((obs, vecchio, nuovo) -> {
            if (aggiornamentoInterno || nuovo == null) {
                return;
            }
            aggiornamentoInterno = true;
            selettoreTitolo.getEditor().setText(nuovo.getTitolo());
            selettoreTitolo.getEditor().positionCaret(nuovo.getTitolo().length());
            aggiornamentoInterno = false;
            filmRiconosciuto = nuovo;
            campoTitolo.pulisciErrore();
            aggiornaFinestreLibere(null);
        });

        selettoreTitolo.getEditor().textProperty().addListener((obs, vecchio, nuovo) -> {
            if (aggiornamentoInterno) {
                return;
            }
            if (filmRiconosciuto != null
                    && normalizza(nuovo).equals(normalizza(filmRiconosciuto.getTitolo()))) {
                return;
            }
            verificaTitolo(nuovo);
        });
    }

    private void caricaSuggerimenti() {
        try {
            List<Film> tutti = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni().elencaFilm();
            selettoreTitolo.setItems(FXCollections.observableArrayList(tutti));
        } catch (RemoteException ex) {
            mostraErrore("Impossibile caricare l'elenco dei film: " + ex.getMessage());
        }
    }

    private void configuraCampoCosto() {
        TextField tf = (TextField) campoCosto.getControllo();
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String nuovo = change.getControlNewText();
            if (nuovo.isEmpty()) {
                return change;
            }
            if (nuovo.matches("\\d*([.,]\\d{0,2})?")) {
                return change;
            }
            return null;
        });
        tf.setTextFormatter(formatter);
    }

    private void configuraSelettoreOra() {
        selettoreOra.setMaxWidth(Double.MAX_VALUE);
        selettoreOra.setVisibleRowCount(8);
        selettoreOra.setPromptText("Scegli film e data");
        selettoreOra.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(FORMATO_ORA);
            }

            @Override
            public LocalTime fromString(String s) {
                return null;
            }
        });
        selettoreOra.setDisable(true);
    }

    private void configuraDatePicker() {
        DatePicker dp = (DatePicker) campoData.getControllo();
        dp.valueProperty().addListener((obs, vecchia, nuova) -> {
            if (!aggiornamentoInterno) {
                aggiornaFinestreLibere(null);
            }
        });
    }

    private GridPane costruisciGriglia() {
        GridPane griglia = new GridPane();
        griglia.setAlignment(Pos.CENTER);
        griglia.setHgap(24);
        griglia.setVgap(12);
        griglia.setMaxWidth(LARGHEZZA_GRIGLIA);

        griglia.add(campoTitolo, 0, 0);
        griglia.add(campoCosto, 1, 0);
        griglia.add(campoData, 0, 1);
        griglia.add(campoOra, 1, 1);

        Region stacco = new Region();
        stacco.setMinHeight(8);
        griglia.add(stacco, 0, 2);

        return griglia;
    }

    private String normalizza(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void verificaTitolo(String testo) {
        Film precedente = filmRiconosciuto;
        filmRiconosciuto = null;

        String normalizzato = normalizza(testo);

        if (normalizzato.isEmpty()) {
            campoTitolo.pulisciErrore();
            if (precedente != null) {
                aggiornaFinestreLibere(null);
            }
            return;
        }

        try {
            List<Film> candidati = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni().cercaFilmPerTitolo(testo.trim());
            for (Film f : candidati) {
                if (normalizza(f.getTitolo()).equals(normalizzato)) {
                    filmRiconosciuto = f;
                    break;
                }
            }
        } catch (RemoteException ex) {
            campoTitolo.mostraErrore("Impossibile verificare il titolo: " + ex.getMessage());
            return;
        }

        if (filmRiconosciuto == null) {
            campoTitolo.mostraErrore("Il titolo \"" + testo.trim()
                    + "\" non corrisponde a nessun film esistente.");
        } else {
            campoTitolo.pulisciErrore();
        }
        aggiornaFinestreLibere(null);
    }

    /*
     Ricalcola le finestre libere per il film e la data scelti, ESCLUDENDO la proiezione in
     modifica (così il suo slot resta selezionabile). Se oraDaPreselezionare non è null e
     rientra tra le finestre, viene preselezionata; altrimenti si tenta di mantenere quella
     già scelta.
     */
    private void aggiornaFinestreLibere(LocalTime oraDaPreselezionare) {
        LocalDate data = ((DatePicker) campoData.getControllo()).getValue();
        LocalTime oraPrecedente = oraDaPreselezionare != null
                ? oraDaPreselezionare
                : selettoreOra.getValue();
        selettoreOra.getItems().clear();
        selettoreOra.setValue(null);

        if (filmRiconosciuto == null || data == null) {
            selettoreOra.setDisable(true);
            selettoreOra.setPromptText("Scegli film e data");
            return;
        }

        List<LocalTime> finestre;
        try {
            finestre = gestoreScene.getFornitoreServizi().getServizioProiezioni()
                    .finestreLibere(data, filmRiconosciuto.getDurataMinuti(), dataOraAttuale);
        } catch (RemoteException ex) {
            selettoreOra.setDisable(true);
            mostraErrore("Impossibile calcolare gli orari liberi: " + ex.getMessage());
            return;
        }

        if (finestre.isEmpty()) {
            selettoreOra.setDisable(true);
            selettoreOra.setPromptText("Nessuna finestra libera in questa data");
        } else {
            selettoreOra.setItems(FXCollections.observableArrayList(finestre));
            selettoreOra.setDisable(false);
            selettoreOra.setPromptText("Scegli un orario");
            if (oraPrecedente != null && finestre.contains(oraPrecedente)) {
                selettoreOra.setValue(oraPrecedente);
            }
        }
    }

    private void salva() {
        pulisciMessaggio();
        campoCosto.pulisciErrore();
        campoData.pulisciErrore();
        campoOra.pulisciErrore();

        boolean valido = true;

        LocalTime ora = selettoreOra.getValue();

        verificaTitolo(selettoreTitolo.getEditor().getText());
        if (filmRiconosciuto == null) {
            valido = false;
        }

        Double costo = leggiCosto();
        if (costo == null) {
            campoCosto.evidenziaErrore();
            valido = false;
        }

        LocalDate data = ((DatePicker) campoData.getControllo()).getValue();
        if (data == null) {
            campoData.evidenziaErrore();
            valido = false;
        }

        if (ora == null) {
            campoOra.evidenziaErrore();
            valido = false;
        }

        if (!valido) {
            mostraErrore("Controlla i campi: titolo film valido, costo, data e orario sono obbligatori.");
            return;
        }

        LocalDateTime nuovaDataOra = LocalDateTime.of(data, ora);

        // Niente da fare se nulla è cambiato.
        if (nuovaDataOra.equals(dataOraAttuale)
                && filmRiconosciuto.getIdFilm() == proiezioneOriginale.getFilm().getIdFilm()
                && costo == proiezioneOriginale.getCostoBiglietto()) {
            mostraErrore("Nessuna modifica da salvare.");
            return;
        }

        try {
            boolean ok = gestoreScene.getFornitoreServizi().getServizioProiezioni()
                    .modificaProiezione(dataOraAttuale, filmRiconosciuto.getIdFilm(), nuovaDataOra, costo);
            if (ok) {
                layout.mostraScelta(
                        "Proiezione modificata con successo.",
                        "Torna alla lista", "Resta qui",
                        layout::mostraDashboardRicercaPubblica,
                        null);
            } else {
                // Il server rifiuta se: la proiezione ha prenotazioni, oppure la nuova
                // collocazione si sovrappone a un'altra proiezione.
                mostraErrore("Modifica rifiutata. Possibili cause: la proiezione ha già "
                        + "prenotazioni (non modificabile), oppure il nuovo orario si "
                        + "sovrappone a un'altra proiezione.");
                aggiornaFinestreLibere(null);
            }
        } catch (RemoteException ex) {
            mostraErrore("Errore di comunicazione col server: " + ex.getMessage());
        }
    }

    // Se la proiezione risulta già prenotata (posti liberi < capienza), avvisa subito che
    // non sarà modificabile: il server rifiuterà comunque, ma è meglio dirlo prima.
    private void avvisaSePrenotata() {
        int postiLiberi = proiezioneOriginale.getPostiLiberi();
        if (postiLiberi < CAPIENZA_SALA) {
            mostraErrore("Attenzione: questa proiezione ha già prenotazioni e potrebbe non "
                    + "essere modificabile. Il server rifiuterà la modifica se esistono "
                    + "prenotazioni associate.");
        }
    }

    private Double leggiCosto() {
        String testo = ((TextField) campoCosto.getControllo()).getText();
        if (testo == null || testo.isBlank()) {
            return null;
        }
        try {
            double valore = Double.parseDouble(testo.trim().replace(',', '.'));
            return valore >= 0 ? valore : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void pulisciErroriCampi() {
        campoTitolo.pulisciErrore();
        campoCosto.pulisciErrore();
        campoData.pulisciErrore();
        campoOra.pulisciErrore();
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
