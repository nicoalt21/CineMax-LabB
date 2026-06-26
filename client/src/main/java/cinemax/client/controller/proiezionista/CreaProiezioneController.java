/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.controller.proiezionista;

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
 Schermata "Crea proiezione" del proiezionista, costruita interamente in codice Java e
 con la stessa formattazione della schermata di registrazione (CampoConEtichetta in una
 griglia a due colonne, larghezze e classi CSS condivise).

 Controlli realizzati:
  - TITOLO FILM: campo testuale con suggerimenti (ComboBox editabile) popolati in tempo
    reale da ServizioProiezioni.cercaFilmPerTitolo. Il film e' identificato dalla PK
    idFilm: il titolo serve solo per cercare. Finche' il testo non coincide esattamente
    (case-insensitive) col titolo di un film esistente, il titolo non e' valido e compare
    un errore.
  - COSTO: solo numeri, >= 0, al massimo 2 decimali (centesimi). Il TextFormatter
    impedisce gia' di digitare lettere o una terza cifra decimale.
  - DATA: scelta con DatePicker.
  - ORARIO: NON si digita. Si sceglie da una ComboBox di finestre libere (slot ogni 5
    minuti) calcolate da ServizioProiezioni.finestreLibere per il film e la data scelti,
    tenendo conto della durata del film e delle altre proiezioni gia' programmate.
 */
public class CreaProiezioneController extends DashboardBaseController {

    private static final double LARGHEZZA_CAMPO = 260;
    private static final double LARGHEZZA_GRIGLIA = LARGHEZZA_CAMPO * 2 + 24;
    private static final DateTimeFormatter FORMATO_ORA = DateTimeFormatter.ofPattern("HH:mm");
    // In creazione non si esclude nessuna proiezione dal calcolo delle finestre.
    private static final LocalDateTime NESSUNA_ESCLUSIONE = null;

    private final GestoreScene gestoreScene;

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

    // Film canonico corrispondente al titolo digitato/scelto (null finche' non e' valido).
    private Film filmRiconosciuto = null;
    // Evita rientri durante l'aggiornamento programmatico dei suggerimenti.
    private boolean aggiornamentoInterno = false;

    public CreaProiezioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
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

        Label titolo = new Label("Crea proiezione");
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

        Button btnCrea = new Button("Crea proiezione");
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

    // ComboBox editabile per il titolo: mostra TUTTI i film come suggerimenti (caricati una
    // sola volta) e si risolve a un Film tramite la PK idFilm. Il testo digitato resta
    // sempre visibile nell'editor; la validazione è in tempo reale sul testo.
    private void configuraSelettoreTitolo() {
        selettoreTitolo.setEditable(true);
        selettoreTitolo.setMaxWidth(Double.MAX_VALUE);
        selettoreTitolo.setVisibleRowCount(6);
        selettoreTitolo.getEditor().setPromptText("Es. Inception");

        // Nell'editor si mostra il solo titolo; nella tendina titolo + anno (disambigua
        // eventuali omonimi: il film resta identificato da idFilm).
        selettoreTitolo.setConverter(new StringConverter<Film>() {
            @Override
            public String toString(Film f) {
                return f == null ? "" : f.getTitolo();
            }

            @Override
            public Film fromString(String s) {
                return null; // la risoluzione avviene in verificaTitolo()
            }
        });
        selettoreTitolo.setCellFactory(lv -> new javafx.scene.control.ListCell<Film>() {
            @Override
            protected void updateItem(Film f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? null : f.getTitolo() + "  (" + f.getAnno() + ")");
            }
        });

        // Quando l'utente sceglie un suggerimento dalla tendina (selectedItem è la fonte
        // autorevole della scelta): scrivo il titolo nell'editor e risolvo il film.
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
            aggiornaFinestreLibere();
        });

        // Controllo in tempo reale durante la digitazione nell'editor.
        selettoreTitolo.getEditor().textProperty().addListener((obs, vecchio, nuovo) -> {
            if (aggiornamentoInterno) {
                return;
            }
            // Se il testo coincide già col film selezionato non rivalidare: evita di
            // azzerare la scelta appena fatta dalla tendina.
            if (filmRiconosciuto != null
                    && normalizza(nuovo).equals(normalizza(filmRiconosciuto.getTitolo()))) {
                return;
            }
            verificaTitolo(nuovo);
        });
    }

    // Carica una sola volta tutti i film nella tendina dei suggerimenti.
    private void caricaSuggerimenti() {
        try {
            List<Film> tutti = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni().elencaFilm();
            selettoreTitolo.setItems(FXCollections.observableArrayList(tutti));
        } catch (RemoteException ex) {
            mostraErrore("Impossibile caricare l'elenco dei film: " + ex.getMessage());
        }
    }

    // Costo: solo cifre, >= 0, al massimo 2 decimali. Il filtro blocca input non validi.
    private void configuraCampoCosto() {
        TextField tf = (TextField) campoCosto.getControllo();
        // Ammette: vuoto, interi, oppure numero con max 2 decimali (punto o virgola).
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String nuovo = change.getControlNewText();
            if (nuovo.isEmpty()) {
                return change;
            }
            // 0 o più cifre, eventuale separatore decimale, al massimo 2 cifre decimali.
            if (nuovo.matches("\\d*([.,]\\d{0,2})?")) {
                return change;
            }
            return null; // rifiuta la modifica
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
        // Ricalcola le finestre quando cambia la data.
        dp.valueProperty().addListener((obs, vecchia, nuova) -> aggiornaFinestreLibere());
    }

    // Griglia a due colonne speculare alla schermata di registrazione.
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

    @Override
    public void aggiornaDati() {
        aggiornamentoInterno = true;
        selettoreTitolo.setValue(null);
        selettoreTitolo.getEditor().clear();
        aggiornamentoInterno = false;
        caricaSuggerimenti(); // ricarica l'elenco film (può essere cambiato es. dopo Crea film)

        ((TextField) campoCosto.getControllo()).clear();
        ((DatePicker) campoData.getControllo()).setValue(LocalDate.now());

        selettoreOra.getItems().clear();
        selettoreOra.setValue(null);
        selettoreOra.setDisable(true);
        selettoreOra.setPromptText("Scegli film e data");

        filmRiconosciuto = null;
        pulisciErroriCampi();
        pulisciMessaggio();
    }

    // Normalizza un titolo per il confronto: minuscolo e spazi ridondanti rimossi.
    private String normalizza(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /*
     Controllo in tempo reale del titolo. Cerca tra i film (per titolo) quello il cui titolo
     normalizzato coincide esattamente col testo digitato: se lo trova memorizza il film
     (PK) e abilita il calcolo delle finestre, altrimenti segnala che il titolo non è
     valido. NON ricostruisce la tendina dei suggerimenti (caricata una volta sola), così
     il testo digitato resta sempre visibile.
     */
    private void verificaTitolo(String testo) {
        Film precedente = filmRiconosciuto;
        filmRiconosciuto = null;

        String normalizzato = normalizza(testo);

        if (normalizzato.isEmpty()) {
            campoTitolo.pulisciErrore();
            if (precedente != null) {
                aggiornaFinestreLibere(); // il titolo si è svuotato: niente film
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
                    + "\" non corrisponde a nessun film esistente. "
                    + "Scegli un suggerimento o crea prima il film.");
        } else {
            campoTitolo.pulisciErrore();
        }
        aggiornaFinestreLibere();
    }

    /*
     Ricalcola le finestre libere in base al film riconosciuto e alla data scelta. Se manca
     uno dei due, la ComboBox degli orari resta disabilitata e vuota.
     */
    private void aggiornaFinestreLibere() {
        LocalDate data = ((DatePicker) campoData.getControllo()).getValue();
        LocalTime oraSelezionata = selettoreOra.getValue(); // da preservare se ancora valida
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
                    .finestreLibere(data, filmRiconosciuto.getDurataMinuti(), NESSUNA_ESCLUSIONE);
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
            // Mantiene l'orario già scelto se rientra ancora tra le finestre disponibili.
            if (oraSelezionata != null && finestre.contains(oraSelezionata)) {
                selettoreOra.setValue(oraSelezionata);
            }
        }
    }

    private void crea() {
        pulisciMessaggio();
        campoCosto.pulisciErrore();
        campoData.pulisciErrore();
        campoOra.pulisciErrore();

        boolean valido = true;

        // Leggo l'orario PRIMA di rivalidare il titolo: la rivalidazione ricalcola le
        // finestre e azzererebbe la scelta dell'orario.
        LocalTime ora = selettoreOra.getValue();

        // Rivaluta il titolo al momento della conferma.
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

        LocalDateTime dataOra = LocalDateTime.of(data, ora);
        // postiLiberi a 0: il servizio finto lo imposta alla capienza della sala.
        Proiezione nuova = new Proiezione(0, filmRiconosciuto, dataOra, costo, 0);

        try {
            boolean ok = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni().aggiungiProiezione(nuova);
            if (ok) {
                mostraSuccesso("Proiezione creata: " + filmRiconosciuto.getTitolo()
                        + " il " + data + " alle " + ora.format(FORMATO_ORA) + ".");
                aggiornaDati();
            } else {
                // Caso raro: l'orario scelto è diventato occupato fra il calcolo e il submit.
                mostraErrore("Creazione rifiutata: l'orario non è più disponibile. Riprova.");
                aggiornaFinestreLibere();
            }
        } catch (RemoteException ex) {
            mostraErrore("Errore di comunicazione col server: " + ex.getMessage());
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

    private void mostraSuccesso(String messaggio) {
        labelMessaggio.getStyleClass().setAll("testo-secondario");
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
