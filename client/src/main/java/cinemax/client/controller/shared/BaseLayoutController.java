package cinemax.client.controller.shared;

import cinemax.client.gui.navigation.GestoreScene;
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
import cinemax.common.model.Utente;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    }

    private void costruisciLayout() {
        contenitorePrincipale.getStyleClass().add("sfondo-principale");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));

        Label logo = new Label("CineMax");
        logo.getStyleClass().add("titolo-principale");
        logo.setStyle("-fx-font-size: 22px;");

        labelBenvenuto.getStyleClass().add("testo-secondario");

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);

        btnAuth.setOnAction(this::onAuthCliccato);

        configuraIndicatoreConnessione();

        header.getChildren().addAll(logo, labelBenvenuto, spazio, labelStatoConnessione, btnAuth);

        // Bottone collassa/espandi: sta in alto a destra DENTRO il pannello laterale.
        btnCollassaMenu.getStyleClass().add("bottone-collassa");
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

    // Collassa o espande il menu laterale. Si nasconde solo il contenitore delle voci,
    // mentre il bottone in alto a destra resta visibile e passa da "<" a ">".
    private void toggleMenuLaterale() {
        menuCollassato = !menuCollassato;
        contenutoVoci.setVisible(!menuCollassato);
        contenutoVoci.setManaged(!menuCollassato);
        btnCollassaMenu.setText(menuCollassato ? ">" : "<");
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
