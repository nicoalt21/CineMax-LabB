package cinemax.client.controller.shared;

import cinemax.client.gui.navigation.GestoreScene;
import cinemax.client.gui.util.FasciaEta;
import cinemax.client.service.StatoConnessione;
import cinemax.client.controller.cliente.DashboardClienteController;
import cinemax.client.controller.cliente.MiePrenotazioniController;
import cinemax.client.controller.cliente.PrenotazioneController;
import cinemax.client.controller.bigliettaio.ProiezioniOggiController;
import cinemax.client.controller.bigliettaio.VerificaBigliettoController;
import cinemax.client.controller.proiezionista.CreaProiezioneController;
import cinemax.client.controller.proiezionista.CreaFilmController;
import cinemax.client.controller.proiezionista.ModificaProiezioneController;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Utente;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Circle;
import javafx.geometry.Orientation;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 Guscio comune a Client e Guest, costruito interamente in codice Java.
 Header in alto (con il bottone Login/Logout), menu laterale a sinistra, area centrale dove vengono caricate le dashboard.

 La vista del Guest è identica a quella del Client: l'unica differenza è che le parti riservate vengono attenuate e rese non cliccabili.
 Il bottone in alto a destra diventa "Log In" (primario) invece di "Log Out" (bianco).

 Il guscio ospita anche un overlay di conferma interno (mostraConferma): le richieste di
 conferma (logout, annullamento prenotazione, ...) appaiono come pannello sopra il
 contenuto, DENTRO la finestra dell'app, senza aprire finestre di dialogo esterne.
 */
public class BaseLayoutController {

    // Radice effettiva: uno StackPane che sovrappone l'overlay di conferma al layout.
    private final StackPane radice = new StackPane();
    private final BorderPane contenitorePrincipale = new BorderPane();
    private final VBox menuLaterale = new VBox(10);
    private final Label labelBenvenuto = new Label();

    // Contenitore di sinistra (pannello menu) e parte collassabile (le voci di menu).
    private final HBox contenitoreSinistro = new HBox();
    private final HBox pannelloMenu = new HBox();
    private final VBox contenutoVoci = new VBox(10);
    private final Button btnCollassaMenu = new Button("<");
    private boolean menuCollassato = false;

    // Overlay di conferma (nascosto finché non serve).
    private final StackPane overlayConferma = new StackPane();
    private final Label labelMessaggioConferma = new Label();
    private final Button btnConfermaSi = new Button("Sì");
    private final Button btnConfermaNo = new Button("No");

    // Bottone Login/Logout (cambia testo e stile in base allo stato)
    private final Button btnAuth = new Button();

    // Overlay dei dettagli di una proiezione (nascosto finche' non serve): uno strato
    // sopra il contenuto con sfondo scurito, che centra il riquadro coi dati. Cliccando
    // fuori dal riquadro (sullo sfondo) il pannello si chiude. Costruito una volta sola
    // e ripopolato a ogni apertura.
    private final StackPane overlayDettagli = new StackPane();
    private final VBox riquadroDettagli = new VBox(16);
    private final Label dettagliTitolo = new Label();
    private final Circle dettagliPallinoEta = new Circle(7);
    private final Label dettagliEta = new Label();
    private final Label dettagliGenere = new Label();
    private final Label dettagliRegista = new Label();
    private final Label dettagliAnno = new Label();
    private final Label dettagliDurata = new Label();
    private final Label dettagliDataOra = new Label();
    private final Label dettagliFine = new Label();
    private final Label dettagliPrezzo = new Label();
    private final Label dettagliPosti = new Label();
    private final Label dettagliAvviso = new Label();
    private final Button dettagliBtnPrenota = new Button("Prenota");
    // Proiezione attualmente mostrata nel pannello dettagli.
    private Proiezione proiezioneDettaglio;

    // Overlay dei dettagli di una PRENOTAZIONE (nascosto finche' non serve): stesso schema
    // dell'overlay dettagli proiezione (sfondo scurito + riquadro centrato). Mostra le info
    // della prenotazione col codice ben visibile e copiabile, e i bottoni Annulla/Modifica.
    private final StackPane overlayPrenotazione = new StackPane();
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
    // Prenotazione attualmente mostrata nell'overlay e callback per annulla/modifica,
    // forniti da chi apre l'overlay (MiePrenotazioniController).
    private Prenotazione prenotazioneDettaglio;
    private java.util.function.Consumer<Prenotazione> azioneAnnullaPrenotazione;
    private java.util.function.Consumer<Prenotazione> azioneModificaPrenotazione;

    // Indicatore di stato della connessione al server, sempre visibile nell'header.
    // Il testo e lo stile (verde/rosso) seguono StatoConnessione.connessoProperty().
    private final Label labelStatoConnessione = new Label();

    // Nodi che il guest vede ma non può usare (es. "Prenota", "Le mie prenotazioni")
    private final List<Node> nodiRiservati = new ArrayList<>();

    // Stato
    private Utente utenteLoggato;
    private boolean isGuest;

    private final GestoreScene gestoreScene;

    public BaseLayoutController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        costruisciLayout();
        costruisciOverlayConferma();
        costruisciOverlayDettagli();
        costruisciOverlayPrenotazione();
    }

    private void costruisciLayout() {
        contenitorePrincipale.getStyleClass().add("sfondo-principale");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));

        Label logo = new Label("🎬 CineMax");
        logo.getStyleClass().add("titolo-principale");
        logo.setStyle("-fx-font-size: 22px;");

        labelBenvenuto.getStyleClass().add("testo-secondario");

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);

        btnAuth.setOnAction(this::onAuthCliccato);

        configuraIndicatoreConnessione();

        header.getChildren().addAll(logo, labelBenvenuto, spazio, labelStatoConnessione, btnAuth);

        // Bottone collassa/espandi: sta in alto a destra DENTRO il pannello laterale.
        btnCollassaMenu.getStyleClass().add("bottone-collassa-menu");
        btnCollassaMenu.setOnAction(e -> toggleMenuLaterale());
        Region spintaToggle = new Region();
        HBox.setHgrow(spintaToggle, Priority.ALWAYS);
        HBox rigaToggle = new HBox(spintaToggle, btnCollassaMenu);
        rigaToggle.setAlignment(Pos.TOP_RIGHT);

        // Menu Laterale (senza contorno): in cima la riga col bottone di chiusura
        // (sempre visibile), poi il contenitore collassabile con le voci di menu.
        menuLaterale.setPadding(new Insets(10, 12, 20, 12));
        menuLaterale.setAlignment(Pos.TOP_LEFT);
        menuLaterale.setPrefWidth(200);
        HBox.setHgrow(menuLaterale, Priority.ALWAYS);
        VBox.setVgrow(contenutoVoci, Priority.ALWAYS);
        menuLaterale.getChildren().addAll(rigaToggle, contenutoVoci);

        // Pannello collassabile: contiene solo il menu (nessun divisore/contorno).
        pannelloMenu.getChildren().add(menuLaterale);

        contenitoreSinistro.setAlignment(Pos.TOP_LEFT);
        contenitoreSinistro.getChildren().add(pannelloMenu);

        contenitorePrincipale.setTop(header);
        contenitorePrincipale.setLeft(contenitoreSinistro);

        // Il layout vero sta sotto; l'overlay (aggiunto dopo) sta sopra.
        radice.getChildren().add(contenitorePrincipale);
    }

    /*
     Configura l'indicatore di stato della connessione mostrato nell'header.
     Si lega a StatoConnessione.connessoProperty(): quando lo stato cambia,
     aggiorna testo e colore (verde = connesso, rosso = disconnesso). Il pallino
     "●" davanti al testo rende l'indicatore leggibile a colpo d'occhio.
     La property viene già aggiornata sul thread JavaFX da StatoConnessione,
     quindi il listener può toccare la UI senza ulteriori accorgimenti.
    */
    private void configuraIndicatoreConnessione() {
        labelStatoConnessione.getStyleClass().add("testo-secondario");
        StatoConnessione stato = StatoConnessione.getInstance();
        aggiornaIndicatoreConnessione(stato.isConnesso());
        stato.connessoProperty().addListener(
                (obs, eraConnesso, oraConnesso) -> aggiornaIndicatoreConnessione(oraConnesso));
    }

    // Applica testo e colore all'indicatore in base allo stato corrente.
    private void aggiornaIndicatoreConnessione(boolean connesso) {
        if (connesso) {
            labelStatoConnessione.setText("● Connesso");
            labelStatoConnessione.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            labelStatoConnessione.setText("● Disconnesso");
            labelStatoConnessione.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        }
    }

    // Costruisce l'overlay di conferma: sfondo scuro semi-trasparente + riquadro centrale
    // con messaggio e due bottoni. Resta nascosto finché non lo si richiama.
    private void costruisciOverlayConferma() {
        overlayConferma.getStyleClass().add("overlay-conferma");

        VBox riquadro = new VBox(20);
        riquadro.getStyleClass().add("riquadro-conferma");
        riquadro.setAlignment(Pos.CENTER);
        riquadro.setMaxWidth(380);
        riquadro.setMaxHeight(Region.USE_PREF_SIZE);
        riquadro.setPadding(new Insets(25));

        labelMessaggioConferma.getStyleClass().add("testo-normale");
        labelMessaggioConferma.setWrapText(true);
        labelMessaggioConferma.setAlignment(Pos.CENTER);

        btnConfermaSi.getStyleClass().add("bottone-primario");
        btnConfermaNo.getStyleClass().add("bottone-secondario");
        HBox rigaBottoni = new HBox(12, btnConfermaSi, btnConfermaNo);
        rigaBottoni.setAlignment(Pos.CENTER);

        riquadro.getChildren().addAll(labelMessaggioConferma, rigaBottoni);
        overlayConferma.getChildren().add(riquadro);

        // Nascosto di default.
        overlayConferma.setVisible(false);
        overlayConferma.setManaged(false);

        radice.getChildren().add(overlayConferma);
    }

    public Parent getRoot() {
        return radice;
    }

    /*
     Mostra l'overlay di conferma con il messaggio dato. Se l'utente preme "Sì" viene
     eseguita azioneSuConferma; in entrambi i casi l'overlay si chiude. È il modo con
     cui tutte le schermate chiedono conferma rimanendo dentro la finestra dell'app.
     */
    public void mostraConferma(String messaggio, Runnable azioneSuConferma) {
        mostraScelta(messaggio, "Sì", "No", azioneSuConferma, null);
    }

    /*
     Variante generale dell'overlay: messaggio + due bottoni con etichette e azioni
     personalizzabili. Usata, ad esempio, per il Guest che clicca "Profilo" e può
     scegliere fra "Accedi" e "Registrati". Passare null a un'azione la rende un semplice
     "chiudi".
     */
    public void mostraScelta(String messaggio, String testoSi, String testoNo,
                             Runnable azioneSi, Runnable azioneNo) {
        labelMessaggioConferma.setText(messaggio);
        btnConfermaSi.setText(testoSi);
        btnConfermaNo.setText(testoNo);
        btnConfermaSi.setOnAction(e -> {
            chiudiConferma();
            if (azioneSi != null) {
                azioneSi.run();
            }
        });
        btnConfermaNo.setOnAction(e -> {
            chiudiConferma();
            if (azioneNo != null) {
                azioneNo.run();
            }
        });
        overlayConferma.setVisible(true);
        overlayConferma.setManaged(true);
    }

    private void chiudiConferma() {
        overlayConferma.setVisible(false);
        overlayConferma.setManaged(false);
    }

    /*
     Costruisce l'overlay centrale dei dettagli proiezione. Stessa struttura dell'overlay
     di conferma: uno strato a tutta superficie con sfondo scurito che centra il riquadro
     coi dati. Cliccando sullo sfondo (fuori dal riquadro) il pannello si chiude; il
     contenuto e' dentro uno ScrollPane per restare comodo anche su finestre molto basse.
     Resta nascosto finche' mostraDettagliProiezione(...) non lo popola e lo rende visibile.
    */
    private void costruisciOverlayDettagli() {
        // Intestazione: titolo del film + bottone di chiusura "X".
        dettagliTitolo.getStyleClass().add("titolo-principale");
        dettagliTitolo.setStyle("-fx-font-size: 24px;");
        dettagliTitolo.setWrapText(true);

        dettagliEta.getStyleClass().add("testo-secondario");
        HBox rigaEta = new HBox(8, dettagliPallinoEta, dettagliEta);
        rigaEta.setAlignment(Pos.CENTER_LEFT);

        Button btnChiudi = new Button("X");
        btnChiudi.getStyleClass().add("bottone-collassa");
        btnChiudi.setOnAction(e -> chiudiDettagli());
        Region spintaChiudi = new Region();
        HBox.setHgrow(spintaChiudi, Priority.ALWAYS);
        HBox rigaIntestazione = new HBox(10, dettagliTitolo, spintaChiudi, btnChiudi);
        rigaIntestazione.setAlignment(Pos.TOP_LEFT);

        // Dettagli del film e della proiezione, ognuno su una riga etichettata.
        for (Label l : new Label[]{dettagliGenere, dettagliRegista, dettagliAnno,
                dettagliDurata, dettagliDataOra, dettagliFine, dettagliPrezzo, dettagliPosti}) {
            l.getStyleClass().add("testo-normale");
            l.setWrapText(true);
        }

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.getStyleClass().add("divisore-menu");

        dettagliAvviso.getStyleClass().add("testo-secondario");
        dettagliAvviso.setWrapText(true);
        dettagliAvviso.setManaged(false);
        dettagliAvviso.setVisible(false);

        dettagliBtnPrenota.getStyleClass().add("bottone-primario");
        dettagliBtnPrenota.setMaxWidth(Double.MAX_VALUE);
        dettagliBtnPrenota.setOnAction(e -> {
            Proiezione p = proiezioneDettaglio;
            chiudiDettagli();
            if (p != null) {
                mostraPrenotazione(p);
            }
        });

        riquadroDettagli.getStyleClass().add("riquadro-conferma");
        riquadroDettagli.setPadding(new Insets(16));
        riquadroDettagli.setPrefWidth(360);
        riquadroDettagli.setMaxWidth(360);
        // Riquadro centrato: si dimensiona sul contenuto, senza occupare tutta l'altezza.
        riquadroDettagli.setMaxHeight(Region.USE_PREF_SIZE);
        riquadroDettagli.getChildren().addAll(
                rigaIntestazione, rigaEta, sep,
                dettagliGenere, dettagliRegista, dettagliAnno, dettagliDurata,
                dettagliDataOra, dettagliFine, dettagliPrezzo, dettagliPosti,
                dettagliAvviso, dettagliBtnPrenota);

        // Il riquadro sta dentro uno ScrollPane, utile solo se il contenuto supera
        // l'altezza disponibile su finestre molto basse; il blocco resta comunque
        // centrato grazie al wrapper sottostante.
        ScrollPane scrollDettagli = new ScrollPane(riquadroDettagli);
        scrollDettagli.setFitToWidth(true);
        scrollDettagli.getStyleClass().add("area-scroll");
        scrollDettagli.setMaxWidth(360);
        scrollDettagli.setMaxHeight(Region.USE_PREF_SIZE);
        // Lo ScrollPane non deve catturare il click destinato allo sfondo: lo lasciamo
        // grande quanto il riquadro, cosi' il resto dell'overlay resta "sfondo".
        StackPane.setAlignment(scrollDettagli, Pos.CENTER);

        overlayDettagli.getStyleClass().add("overlay-conferma");
        overlayDettagli.setAlignment(Pos.CENTER);
        // Click sullo sfondo scurito (fuori dal riquadro) chiude il pannello. Il click
        // sul riquadro non si propaga qui perche' lo consumiamo sul riquadro stesso.
        overlayDettagli.setOnMouseClicked(e -> chiudiDettagli());
        riquadroDettagli.setOnMouseClicked(javafx.event.Event::consume);
        overlayDettagli.getChildren().add(scrollDettagli);
        overlayDettagli.setVisible(false);
        overlayDettagli.setManaged(false);

        radice.getChildren().add(overlayDettagli);
    }

    /*
     Mostra il pannello laterale coi dettagli della proiezione indicata. Consentito anche
     al Guest (sola consultazione). Il bottone "Prenota" segue la stessa logica delle card:
     bloccato per il Guest, per chi non ha l'eta' minima e per le proiezioni gia' avvenute o
     esaurite; attivo e funzionante per il cliente idoneo. Per il proiezionista i dettagli
     restano in sola lettura (nessun "Prenota").
    */
    public void mostraDettagliProiezione(Proiezione p) {
        if (p == null) {
            return;
        }
        this.proiezioneDettaglio = p;

        cinemax.common.model.Film film = p.getFilm();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

        dettagliTitolo.setText(film.getTitolo());

        FasciaEta.Fascia fascia = FasciaEta.fasciaPerEta(film.getEtaMinima());
        dettagliPallinoEta.getStyleClass().setAll("pallino-eta", fascia.getClasseCss());
        dettagliEta.setText(film.getEtaMinima() <= 0
                ? "Adatto a tutte le età!"
                : "Vietato ai minori di " + film.getEtaMinima() + " anni");

        dettagliGenere.setText("Genere: " + film.getGenere());
        dettagliRegista.setText("Regista: " + film.getRegista());
        dettagliAnno.setText("Anno: " + film.getAnno());
        dettagliDurata.setText("Durata: " + film.getDurataMinuti() + " min");
        dettagliDataOra.setText("Inizio: " + p.getDataOra().format(formato));
        LocalDateTime fine = p.getDataOraFine();
        dettagliFine.setText("Fine prevista: " + (fine != null ? fine.format(formato) : "-"));
        dettagliPrezzo.setText(String.format("Prezzo biglietto: %.2f \u20ac", p.getCostoBiglietto()));

        int posti = p.getPostiLiberi();
        dettagliPosti.getStyleClass().remove("testo-esaurito");
        if (posti <= 0) {
            dettagliPosti.setText("Esaurito");
            dettagliPosti.getStyleClass().add("testo-esaurito");
        } else {
            dettagliPosti.setText(posti + (posti == 1 ? " posto libero" : " posti liberi"));
        }

        configuraBottonePrenotaDettagli(p, posti);

        overlayDettagli.setVisible(true);
        overlayDettagli.setManaged(true);
    }

    /*
     Decide stato ed eventuale avviso del bottone "Prenota" nel pannello dettagli, con la
     stessa logica usata dalle card: il proiezionista non prenota; il Guest e' invitato ad
     accedere; chi non ha l'eta' minima o cerca una proiezione passata/esaurita trova il
     bottone bloccato con la motivazione.
    */
    private void configuraBottonePrenotaDettagli(Proiezione p, int postiLiberi) {
        dettagliAvviso.setManaged(false);
        dettagliAvviso.setVisible(false);
        dettagliBtnPrenota.setDisable(false);
        dettagliBtnPrenota.setManaged(true);
        dettagliBtnPrenota.setVisible(true);
        Tooltip.install(dettagliBtnPrenota, null);

        // Il proiezionista consulta i dettagli ma non prenota: niente bottone.
        if (!isGuest && utenteLoggato != null
                && utenteLoggato.getRuolo() == cinemax.common.model.Ruolo.PROIEZIONISTA) {
            dettagliBtnPrenota.setManaged(false);
            dettagliBtnPrenota.setVisible(false);
            return;
        }

        // Guest: bottone visibile ma bloccato, con invito ad accedere.
        if (isGuest) {
            dettagliBtnPrenota.setDisable(true);
            mostraAvvisoDettagli("Accedi come cliente per prenotare.");
            return;
        }

        // Proiezione gia' avvenuta.
        if (!p.getDataOra().isAfter(LocalDateTime.now())) {
            dettagliBtnPrenota.setDisable(true);
            mostraAvvisoDettagli("Proiezione gia' avvenuta: non prenotabile.");
            return;
        }

        // Esaurita.
        if (postiLiberi <= 0) {
            dettagliBtnPrenota.setDisable(true);
            mostraAvvisoDettagli("Proiezione esaurita.");
            return;
        }

        // Blocco per eta' minima.
        int etaMinima = p.getFilm().getEtaMinima();
        if (!FasciaEta.puoPrenotare(utenteLoggato.getDataNascita(), etaMinima)) {
            dettagliBtnPrenota.setDisable(true);
            mostraAvvisoDettagli("Vietato ai minori di " + etaMinima + " anni.");
        }
    }

    private void mostraAvvisoDettagli(String testo) {
        dettagliAvviso.setText(testo);
        dettagliAvviso.setManaged(true);
        dettagliAvviso.setVisible(true);
    }

    private void chiudiDettagli() {
        overlayDettagli.setVisible(false);
        overlayDettagli.setManaged(false);
        proiezioneDettaglio = null;
    }

    /*
     Costruisce l'overlay centrale dei dettagli di una prenotazione. Stessa impostazione
     dell'overlay dettagli proiezione: sfondo scurito che chiude al click, riquadro
     centrato e scrollabile. Mostra le info della prenotazione, col codice in un campo di
     sola lettura ma selezionabile (comodo da copiare), e i due bottoni Annulla/Modifica.
     I bottoni delegano alle callback fornite da chi apre l'overlay.
    */
    private void costruisciOverlayPrenotazione() {
        prenTitolo.getStyleClass().add("titolo-principale");
        prenTitolo.setStyle("-fx-font-size: 24px;");
        prenTitolo.setWrapText(true);

        prenEta.getStyleClass().add("testo-secondario");
        HBox rigaEta = new HBox(8, prenPallinoEta, prenEta);
        rigaEta.setAlignment(Pos.CENTER_LEFT);

        Button btnChiudi = new Button("X");
        btnChiudi.getStyleClass().add("bottone-collassa");
        btnChiudi.setOnAction(e -> chiudiDettagliPrenotazione());
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

        // Codice ben visibile e copiabile: etichetta sopra + campo di sola lettura,
        // selezionabile, con carattere monospaziato (classe campo-codice).
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
            // Salvo prenotazione e callback in locali PRIMA di chiudere: chiudiDettagli-
            // Prenotazione() azzera i campi, quindi leggerli dopo darebbe sempre null.
            Prenotazione p = prenotazioneDettaglio;
            java.util.function.Consumer<Prenotazione> azione = azioneModificaPrenotazione;
            chiudiDettagliPrenotazione();
            if (p != null && azione != null) {
                azione.accept(p);
            }
        });

        prenBtnAnnulla.getStyleClass().add("bottone-secondario");
        prenBtnAnnulla.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(prenBtnAnnulla, Priority.ALWAYS);
        prenBtnAnnulla.setOnAction(e -> {
            Prenotazione p = prenotazioneDettaglio;
            java.util.function.Consumer<Prenotazione> azione = azioneAnnullaPrenotazione;
            chiudiDettagliPrenotazione();
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
        // Gerarchia: intestazione, eta', divisore, poi il CODICE (in evidenza), quindi le
        // info della proiezione e infine i bottoni impilati.
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

        overlayPrenotazione.getStyleClass().add("overlay-conferma");
        overlayPrenotazione.setAlignment(Pos.CENTER);
        overlayPrenotazione.setOnMouseClicked(e -> chiudiDettagliPrenotazione());
        riquadroPrenotazione.setOnMouseClicked(javafx.event.Event::consume);
        overlayPrenotazione.getChildren().add(scroll);
        overlayPrenotazione.setVisible(false);
        overlayPrenotazione.setManaged(false);

        radice.getChildren().add(overlayPrenotazione);
    }

    /*
     Mostra l'overlay coi dettagli di una prenotazione attiva. Le callback per Annulla e
     Modifica sono fornite dal controller chiamante (MiePrenotazioniController), che sa
     come dialogare col server e come ricaricare la lista dopo l'operazione. L'overlay si
     usa solo per prenotazioni attive: le card passate non sono cliccabili.
    */
    public void mostraDettagliPrenotazione(Prenotazione p,
                                           java.util.function.Consumer<Prenotazione> onAnnulla,
                                           java.util.function.Consumer<Prenotazione> onModifica) {
        if (p == null) {
            return;
        }
        this.prenotazioneDettaglio = p;
        this.azioneAnnullaPrenotazione = onAnnulla;
        this.azioneModificaPrenotazione = onModifica;

        cinemax.common.model.Proiezione proiezione = p.getProiezione();
        cinemax.common.model.Film film = proiezione.getFilm();
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

        overlayPrenotazione.setVisible(true);
        overlayPrenotazione.setManaged(true);
    }

    private void chiudiDettagliPrenotazione() {
        overlayPrenotazione.setVisible(false);
        overlayPrenotazione.setManaged(false);
        prenotazioneDettaglio = null;
        azioneAnnullaPrenotazione = null;
        azioneModificaPrenotazione = null;
    }

    // Vero se il layout sta operando in modalità Guest (utente non autenticato).
    public boolean isGuest() {
        return isGuest;
    }

    public Utente getUtenteLoggato() {
        return utenteLoggato;
    }

    // Inizializza il contesto. utente == null significa Guest.
    public void inizializzaContesto(Utente utente) {
        this.utenteLoggato = utente;
        this.isGuest = (utente == null);

        if (isGuest) {
            labelBenvenuto.setText("Modalità Guest");
        } else {
            labelBenvenuto.setText("Ciao, " + utente.getNome());
        }

        aggiornaStatoAuth();
        generaMenuLaterale();
    }

    // Inserisce una dashboard al centro senza ricostruire header/menu.
    public void impostaContenutoCentrale(Node contenuto) {
        contenitorePrincipale.setCenter(contenuto);
    }

    // Registra un nodo come "riservato": il guest lo vede attenuato e non lo può cliccare.
    // Da chiamare quando costruisci la dashboard, es: layout.registraNodoRiservato(card.getBottonePrincipale());
    public void registraNodoRiservato(Node n) {
        nodiRiservati.add(n);
        applicaStatoNodo(n);
    }

    // Applica lo stato auth corrente a tutti i nodi riservati e al bottone.
    private void aggiornaStatoAuth() {
        if (isGuest) {
            btnAuth.setText("Log In");
            btnAuth.getStyleClass().setAll("bottone-primario");
        } else {
            btnAuth.setText("Log Out");
            btnAuth.getStyleClass().setAll("bottone-bianco");
        }
        for (Node n : nodiRiservati) {
            applicaStatoNodo(n);
        }
    }

    // Attenua + disattiva il click se guest, ripristina se client.
    private void applicaStatoNodo(Node n) {
        if (isGuest) {
            n.setMouseTransparent(true);
            if (!n.getStyleClass().contains("bloccato-guest")) {
                n.getStyleClass().add("bloccato-guest");
            }
        } else {
            n.setMouseTransparent(false);
            n.getStyleClass().remove("bloccato-guest");
        }
    }

    private void generaMenuLaterale() {
        contenutoVoci.getChildren().clear();

        // Voci comuni e specifiche per ruolo. Il Guest vede il menu del Cliente, con le
        // voci riservate attenuate (registraNodoRiservato).
        VBox vociAlto = new VBox(10);

        if (isGuest || utenteLoggato.getRuolo() == cinemax.common.model.Ruolo.CLIENTE) {
            // CLIENTE / GUEST: ricerca, le mie prenotazioni, profilo, impostazioni.
            Button voceCerca = costruisciVoceMenu("Cerca proiezioni");
            voceCerca.setOnAction(e -> mostraDashboardRicerca());

            Button vocePrenotazioni = costruisciVoceMenu("Le mie prenotazioni");
            vocePrenotazioni.setOnAction(e -> mostraMiePrenotazioni());
            registraNodoRiservato(vocePrenotazioni); // riservata: Guest non prenota

            Button voceProfilo = costruisciVoceMenu("Profilo");
            voceProfilo.setOnAction(e -> mostraProfilo());

            Button voceImpostazioni = costruisciVoceMenu("Impostazioni");
            voceImpostazioni.setOnAction(e -> mostraImpostazioni());

            vociAlto.getChildren().addAll(voceCerca, vocePrenotazioni, voceProfilo, voceImpostazioni);

        } else if (utenteLoggato.getRuolo() == cinemax.common.model.Ruolo.PROIEZIONISTA) {
            // PROIEZIONISTA: gestione proiezioni, crea proiezione, crea film, profilo, impostazioni.
            Button voceGestione = costruisciVoceMenu("Gestione proiezioni");
            voceGestione.setOnAction(e -> mostraGestioneProiezioni());

            Button voceCrea = costruisciVoceMenu("Crea proiezione");
            voceCrea.setOnAction(e -> mostraCreaProiezione());

            Button voceCreaFilm = costruisciVoceMenu("Crea film");
            voceCreaFilm.setOnAction(e -> mostraCreaFilm());

            Button voceProfilo = costruisciVoceMenu("Profilo");
            voceProfilo.setOnAction(e -> mostraProfilo());

            Button voceImpostazioni = costruisciVoceMenu("Impostazioni");
            voceImpostazioni.setOnAction(e -> mostraImpostazioni());

            vociAlto.getChildren().addAll(voceGestione, voceCrea, voceCreaFilm, voceProfilo, voceImpostazioni);

        } else {
            // BIGLIETTAIO: proiezioni di oggi, verifica biglietto, profilo, impostazioni.
            Button voceOggi = costruisciVoceMenu("Proiezioni di oggi");
            voceOggi.setOnAction(e -> mostraProiezioniOggi());

            Button voceVerifica = costruisciVoceMenu("Verifica biglietto");
            voceVerifica.setOnAction(e -> mostraVerificaBiglietto());

            Button voceProfilo = costruisciVoceMenu("Profilo");
            voceProfilo.setOnAction(e -> mostraProfilo());

            Button voceImpostazioni = costruisciVoceMenu("Impostazioni");
            voceImpostazioni.setOnAction(e -> mostraImpostazioni());

            vociAlto.getChildren().addAll(voceOggi, voceVerifica, voceProfilo, voceImpostazioni);
        }

        // Spaziatore che spinge l'etichetta ruolo in fondo al menu.
        Region spinta = new Region();
        VBox.setVgrow(spinta, Priority.ALWAYS);

        // Etichetta del ruolo in fondo, centrata nel pannello laterale, preceduta da un
        // divisore orizzontale a tutta larghezza del pannello.
        Separator divisoreRuolo = new Separator(Orientation.HORIZONTAL);
        divisoreRuolo.getStyleClass().add("divisore-menu");

        Label etichettaRuolo = new Label(descrizioneRuolo());
        etichettaRuolo.getStyleClass().add("etichetta-ruolo");
        etichettaRuolo.setMaxWidth(Double.MAX_VALUE);
        etichettaRuolo.setAlignment(Pos.CENTER);

        contenutoVoci.getChildren().addAll(vociAlto, spinta, divisoreRuolo, etichettaRuolo);
    }

    // Collassa o espande il menu laterale. Da collassato il pannello si stringe alla sola
    // larghezza del bottone freccia, cosi' il BorderPane non gli riserva piu' spazio e il
    // contenuto centrale si estende fin verso sinistra; il bottone resta visibile e passa
    // da "<" a ">".
    private void toggleMenuLaterale() {
        menuCollassato = !menuCollassato;
        contenutoVoci.setVisible(!menuCollassato);
        contenutoVoci.setManaged(!menuCollassato);
        btnCollassaMenu.setText(menuCollassato ? ">" : "<");

        if (menuCollassato) {
            // Pannello ridotto alla larghezza minima (solo il bottone): rilascia lo spazio.
            menuLaterale.setPrefWidth(Region.USE_COMPUTED_SIZE);
            menuLaterale.setMinWidth(Region.USE_COMPUTED_SIZE);
            menuLaterale.setMaxWidth(Region.USE_COMPUTED_SIZE);
        } else {
            // Pannello espanso alla larghezza piena.
            menuLaterale.setPrefWidth(200);
            menuLaterale.setMinWidth(Region.USE_COMPUTED_SIZE);
            menuLaterale.setMaxWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    // Testo del ruolo mostrato in basso (Guest incluso).
    private String descrizioneRuolo() {
        if (isGuest) {
            return "Modalità Guest";
        }
        switch (utenteLoggato.getRuolo()) {
            case PROIEZIONISTA: return "Proiezionista";
            case BIGLIETTAIO:   return "Bigliettaio";
            default:            return "Cliente";
        }
    }

    // Carica la schermata Profilo. Per il Guest non esiste un profilo: si offre la
    // scelta fra accedere e registrarsi, restando dentro la finestra dell'app.
    private void mostraProfilo() {
        if (isGuest) {
            mostraScelta(
                    "Non hai un profilo: accedi se hai un account, oppure registrati per crearne uno.",
                    "Accedi", "Registrati",
                    gestoreScene::vaiALogin,
                    gestoreScene::vaiARegistrazione);
            return;
        }
        ProfiloController profilo = new ProfiloController(gestoreScene);
        profilo.setUtente(utenteLoggato);
        profilo.inizializza();
        impostaContenutoCentrale(profilo.getRoot());
    }

    // Placeholder di navigazione: gestione proiezioni del proiezionista.
    // Riusa la dashboard di ricerca, che si adatta al ruolo (titolo "Cerca e modifica"
    // e bottone "Modifica" sulle card).
    private void mostraGestioneProiezioni() {
        mostraDashboardRicerca();
    }

    // Carica la schermata "Crea proiezione" del proiezionista. Riceve this come callback
    // per poter mostrare, dopo la creazione, il feedback in overlay.
    private void mostraCreaProiezione() {
        CreaProiezioneController crea = new CreaProiezioneController(gestoreScene, this);
        crea.setUtente(utenteLoggato);
        crea.inizializza();
        impostaContenutoCentrale(crea.getRoot());
    }

    // Carica la schermata "Crea film" del proiezionista. Riceve this come callback per
    // poter mostrare, dopo la creazione, la richiesta "vuoi creare anche una proiezione?".
    private void mostraCreaFilm() {
        CreaFilmController creaFilm = new CreaFilmController(gestoreScene, this);
        creaFilm.setUtente(utenteLoggato);
        creaFilm.inizializza();
        impostaContenutoCentrale(creaFilm.getRoot());
    }

    // Chiamato dal CreaFilmController dopo aver creato un film: notifica l'esito e
    // propone di creare subito una proiezione per quel film.
    public void notificaFilmCreato(String titoloFilm) {
        mostraScelta(
                "Film \"" + titoloFilm + "\" creato con successo.\nVuoi creare anche una proiezione per questo film?",
                "Sì, crea proiezione", "No, grazie",
                this::mostraCreaProiezione,
                null);
    }

    // Chiamato dal CreaProiezioneController dopo aver creato una proiezione: mostra un
    // feedback evidente in overlay e propone di vedere le proiezioni o crearne un'altra.
    public void notificaProiezioneCreata(String titoloFilm, java.time.LocalDate data, String ora) {
        mostraScelta(
                "Proiezione creata con successo!\n\"" + titoloFilm + "\" il " + data + " alle " + ora + ".",
                "Vai alle proiezioni", "Crea un'altra",
                this::mostraGestioneProiezioni,
                null);
    }

    // Carica la schermata "Proiezioni di oggi" del bigliettaio.
    private void mostraProiezioniOggi() {
        ProiezioniOggiController oggi = new ProiezioniOggiController(gestoreScene);
        oggi.setUtente(utenteLoggato);
        oggi.inizializza();
        impostaContenutoCentrale(oggi.getRoot());
    }

    // Carica la schermata "Verifica biglietto" del bigliettaio.
    private void mostraVerificaBiglietto() {
        VerificaBigliettoController verifica = new VerificaBigliettoController(gestoreScene);
        verifica.setUtente(utenteLoggato);
        verifica.inizializza();
        impostaContenutoCentrale(verifica.getRoot());
    }

    // Carica la schermata "Impostazioni" (comune a tutti i ruoli).
    private void mostraImpostazioni() {
        ImpostazioniController impostazioni = new ImpostazioniController(gestoreScene);
        impostazioni.setUtente(utenteLoggato);
        impostazioni.inizializza();
        impostaContenutoCentrale(impostazioni.getRoot());
    }

    // Sceglie e carica la schermata iniziale in base al ruolo (chiamata all'avvio).
    // titoloInizialeGuest è usato solo per cliente/guest (precompila la ricerca).
    public void mostraDashboardIniziale(String titoloInizialeGuest) {
        if (!isGuest && utenteLoggato.getRuolo() == cinemax.common.model.Ruolo.BIGLIETTAIO) {
            mostraProiezioniOggi();
        } else {
            // Cliente, Guest e Proiezionista partono dalla dashboard di ricerca
            // (che si adatta al ruolo: "Cerca e prenota" o "Cerca e modifica").
            mostraDashboardRicercaConTitolo(titoloInizialeGuest);
        }
    }

    // Carica la dashboard di ricerca proiezioni nell'area centrale (riusa il guscio).
    private void mostraDashboardRicerca() {
        mostraDashboardRicercaConTitolo(null);
    }

    private void mostraDashboardRicercaConTitolo(String titoloInizialeGuest) {
        DashboardClienteController dashboard =
                new DashboardClienteController(gestoreScene, this, titoloInizialeGuest);
        dashboard.setUtente(utenteLoggato);
        dashboard.inizializza();
        impostaContenutoCentrale(dashboard.getRoot());
    }

    // Carica la dashboard "Le mie prenotazioni" nell'area centrale (solo clienti).
    private void mostraMiePrenotazioni() {
        MiePrenotazioniController prenotazioni = new MiePrenotazioniController(gestoreScene, this);
        prenotazioni.setUtente(utenteLoggato);
        prenotazioni.inizializza();
        impostaContenutoCentrale(prenotazioni.getRoot());
    }

    /*
     Apre la schermata di prenotazione per la proiezione scelta. Chiamata dalla
     dashboard di ricerca quando il cliente preme "Prenota" su una card. Il Guest non
     arriva qui perché il layout blocca il bottone di prenotazione; per difesa la
     schermata stessa ricontrolla isGuest().
    */
    public void mostraPrenotazione(Proiezione proiezione) {
        PrenotazioneController prenotazione =
                new PrenotazioneController(gestoreScene, this, proiezione);
        prenotazione.setUtente(utenteLoggato);
        prenotazione.inizializza();
        impostaContenutoCentrale(prenotazione.getRoot());
    }

    /*
     Apre la schermata di modifica di una proiezione (solo proiezionista). Chiamata dalla
     dashboard di ricerca quando il proiezionista preme "Modifica" su una card. La schermata
     rispetta i vincoli di integrità: il server rifiuta la modifica se la proiezione ha
     prenotazioni o se la nuova collocazione si sovrappone.
    */
    public void mostraModificaProiezione(Proiezione proiezione) {
        ModificaProiezioneController modifica =
                new ModificaProiezioneController(gestoreScene, this, proiezione);
        modifica.setUtente(utenteLoggato);
        modifica.inizializza();
        impostaContenutoCentrale(modifica.getRoot());
    }

    /*
     Versioni pubbliche usate dalla schermata di prenotazione per tornare alla ricerca
     o passare alle proprie prenotazioni dopo aver concluso (o annullato) il flusso.
    */
    public void mostraDashboardRicercaPubblica() {
        mostraDashboardRicerca();
    }

    public void mostraMiePrenotazioniPubblica() {
        mostraMiePrenotazioni();
    }

    /*
     Apre la schermata di modifica (spostamento) di una prenotazione: il cliente sceglie
     un'altra proiezione dello stesso film su cui spostare la prenotazione. Chiamata da
     "Le mie prenotazioni" (bottone sulla card o nell'overlay dei dettagli).
    */
    public void mostraModificaPrenotazione(Prenotazione prenotazione) {
        cinemax.client.controller.cliente.ModificaPrenotazioneController modifica =
                new cinemax.client.controller.cliente.ModificaPrenotazioneController(
                        gestoreScene, this, prenotazione);
        modifica.setUtente(utenteLoggato);
        modifica.inizializza();
        impostaContenutoCentrale(modifica.getRoot());
    }

    // Costruisce una voce di menu laterale con lo stile a tutta larghezza.
    private Button costruisciVoceMenu(String testo) {
        Button b = new Button(testo);
        b.getStyleClass().add("voce-menu");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    // Gestisce sia Login (guest) che Logout (client).
    private void onAuthCliccato(ActionEvent e) {
        if (isGuest) {
            gestoreScene.vaiALogin();
        } else {
            // Conferma in-app prima di disconnettere: evita logout accidentali.
            mostraConferma("Stai per essere disconnesso, sei sicuro?", () -> {
                this.utenteLoggato = null;
                gestoreScene.vaiAStart();
            });
        }
    }
}
