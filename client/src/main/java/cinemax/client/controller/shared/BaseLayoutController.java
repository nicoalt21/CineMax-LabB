package cinemax.client.controller.shared;

import cinemax.client.gui.component.overlay.OverlayConferma;
import cinemax.client.gui.component.overlay.OverlayDettagliPrenotazione;
import cinemax.client.gui.component.overlay.OverlayDettagliProiezione;
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
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Utente;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Guscio comune a Client e Guest, costruito interamente in codice Java.
 * Header in alto (con il bottone Login/Logout), menu laterale a sinistra, area centrale dove vengono caricate le dashboard.
 * <p>
 * La vista del Guest è identica a quella del Client: l'unica differenza è che le parti riservate vengono attenuate e rese non cliccabili.
 * Il bottone in alto a destra diventa "Log In" (primario) invece di "Log Out" (bianco).
 * <p>
 * Il guscio ospita anche un overlay di conferma interno (mostraConferma): le richieste di
 * conferma (logout, annullamento prenotazione, ...) appaiono come pannello sopra il
 * contenuto, DENTRO la finestra dell'app, senza aprire finestre di dialogo esterne.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class BaseLayoutController {

    private final StackPane radice = new StackPane();
    private final BorderPane contenitorePrincipale = new BorderPane();
    private final Label labelBenvenuto = new Label();

    private final cinemax.client.gui.component.MenuLaterale pannelloMenuLaterale =
            new cinemax.client.gui.component.MenuLaterale(this);

    private final OverlayConferma overlayConferma = new OverlayConferma();
    private final Button btnAuth = new Button();
    private final OverlayDettagliProiezione overlayDettagli = new OverlayDettagliProiezione();
    private final OverlayDettagliPrenotazione overlayPrenotazione = new OverlayDettagliPrenotazione();
    private final Label labelStatoConnessione = new Label();
    private final List<Node> nodiRiservati = new ArrayList<>();

    private Utente utenteLoggato;
    private boolean isGuest;
    private final GestoreScene gestoreScene;

    /**
     * Inizializza il controller del layout di base.
     *
     * @param gestoreScene Il gestore delle scene per la navigazione.
     */
    public BaseLayoutController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        costruisciLayout();

        overlayConferma.setVisible(false);
        overlayConferma.setManaged(false);
        radice.getChildren().add(overlayConferma);
        radice.getChildren().add(overlayDettagli);
        radice.getChildren().add(overlayPrenotazione);
    }

    /**
     * Costruisce i componenti grafici del layout, inclusi header e menu.
     */
    private void costruisciLayout() {
        contenitorePrincipale.getStyleClass().add("sfondo-principale");

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

        contenitorePrincipale.setTop(header);
        contenitorePrincipale.setLeft(pannelloMenuLaterale);

        radice.getChildren().add(contenitorePrincipale);
    }

    /**
     * Configura l'indicatore di stato della connessione mostrato nell'header.
     * Si lega a StatoConnessione per aggiornamenti in tempo reale.
     */
    private void configuraIndicatoreConnessione() {
        labelStatoConnessione.getStyleClass().add("testo-secondario");
        StatoConnessione stato = StatoConnessione.getInstance();
        aggiornaIndicatoreConnessione(stato.isConnesso());
        stato.connessoProperty().addListener(
                (obs, eraConnesso, oraConnesso) -> aggiornaIndicatoreConnessione(oraConnesso));
    }

    /**
     * Aggiorna visivamente l'indicatore in base allo stato della connessione.
     *
     * @param connesso true se connesso, false altrimenti.
     */
    private void aggiornaIndicatoreConnessione(boolean connesso) {
        if (connesso) {
            labelStatoConnessione.setText("● Connesso");
            labelStatoConnessione.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            labelStatoConnessione.setText("● Disconnesso");
            labelStatoConnessione.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        }
    }

    /**
     * Restituisce il nodo radice del layout.
     *
     * @return Il Parent radice.
     */
    public Parent getRoot() {
        return radice;
    }

    /**
     * Mostra un overlay di conferma semplice.
     *
     * @param messaggio Il messaggio da visualizzare.
     * @param azioneSuConferma L'azione da eseguire alla conferma.
     */
    public void mostraConferma(String messaggio, Runnable azioneSuConferma) {
        overlayConferma.mostraConferma(messaggio, azioneSuConferma);
    }

    /**
     * Mostra un overlay di scelta personalizzato.
     *
     * @param messaggio Il testo principale.
     * @param testoSi Etichetta del bottone primario.
     * @param testoNo Etichetta del bottone secondario.
     * @param azioneSi Azione per il bottone primario.
     * @param azioneNo Azione per il bottone secondario.
     */
    public void mostraScelta(String messaggio, String testoSi, String testoNo,
                             Runnable azioneSi, Runnable azioneNo) {
        overlayConferma.mostraScelta(messaggio, testoSi, testoNo, azioneSi, azioneNo);
    }

    /**
     * Mostra i dettagli di una proiezione e gestisce la prenotazione.
     *
     * @param p La proiezione da mostrare.
     */
    public void mostraDettagliProiezione(Proiezione p) {
        boolean isProiezionista = !isGuest && utenteLoggato != null
                && utenteLoggato.getRuolo() == cinemax.common.model.Ruolo.PROIEZIONISTA;
        java.time.LocalDate dataNascita =
                (utenteLoggato != null) ? utenteLoggato.getDataNascita() : null;
        overlayDettagli.mostra(p, isGuest, isProiezionista, dataNascita, this::mostraPrenotazione);
    }

    /**
     * Mostra l'overlay dei dettagli di una prenotazione attiva.
     *
     * @param p La prenotazione da visualizzare.
     * @param onAnnulla Callback per annullare la prenotazione.
     * @param onModifica Callback per modificare la prenotazione.
     */
    public void mostraDettagliPrenotazione(Prenotazione p,
                                           java.util.function.Consumer<Prenotazione> onAnnulla,
                                           java.util.function.Consumer<Prenotazione> onModifica) {
        overlayPrenotazione.mostra(p, onAnnulla, onModifica);
    }

    /**
     * Verifica se l'utente corrente è un guest.
     *
     * @return true se guest, false altrimenti.
     */
    public boolean isGuest() {
        return isGuest;
    }

    /**
     * Restituisce l'utente attualmente loggato.
     *
     * @return L'istanza dell'utente, oppure null se guest.
     */
    public Utente getUtenteLoggato() {
        return utenteLoggato;
    }

    /**
     * Inizializza il contesto in base all'utente loggato.
     *
     * @param utente L'utente da impostare (null per guest).
     */
    public void inizializzaContesto(Utente utente) {
        this.utenteLoggato = utente;
        this.isGuest = (utente == null);

        if (isGuest) {
            labelBenvenuto.setText("Modalità Guest");
        } else {
            labelBenvenuto.setText("Ciao, " + utente.getNome());
        }

        aggiornaStatoAuth();
        pannelloMenuLaterale.generaMenu();
    }

    /**
     * Imposta il nodo indicato nell'area centrale del layout.
     *
     * @param contenuto Il nodo da visualizzare.
     */
    public void impostaContenutoCentrale(Node contenuto) {
        contenitorePrincipale.setCenter(contenuto);
    }

    /**
     * Registra un nodo come riservato, disabilitandolo se l'utente è guest.
     *
     * @param n Il nodo da registrare.
     */
    public void registraNodoRiservato(Node n) {
        nodiRiservati.add(n);
        applicaStatoNodo(n);
    }

    /**
     * Aggiorna lo stato di tutti i nodi sensibili all'autenticazione.
     */
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

    /**
     * Applica o rimuove la disabilitazione da guest al nodo specifico.
     *
     * @param n Il nodo da aggiornare.
     */
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

    /**
     * Mostra il profilo dell'utente loggato o l'invito a registrarsi se guest.
     */
    public void mostraProfilo() {
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

    /**
     * Carica la dashboard di ricerca per il proiezionista.
     */
    public void mostraGestioneProiezioni() {
        mostraDashboardRicerca();
    }

    /**
     * Mostra la schermata per la creazione di una nuova proiezione.
     */
    public void mostraCreaProiezione() {
        CreaProiezioneController crea = new CreaProiezioneController(gestoreScene, this);
        crea.setUtente(utenteLoggato);
        crea.inizializza();
        impostaContenutoCentrale(crea.getRoot());
    }

    /**
     * Mostra la schermata per l'inserimento di un nuovo film.
     */
    public void mostraCreaFilm() {
        CreaFilmController creaFilm = new CreaFilmController(gestoreScene, this);
        creaFilm.setUtente(utenteLoggato);
        creaFilm.inizializza();
        impostaContenutoCentrale(creaFilm.getRoot());
    }

    /**
     * Notifica l'avvenuta creazione di un film tramite popup.
     *
     * @param titoloFilm Il titolo del film appena creato.
     */
    public void notificaFilmCreato(String titoloFilm) {
        mostraScelta(
                "Film \"" + titoloFilm + "\" creato con successo.\nVuoi creare anche una proiezione per questo film?",
                "Sì, crea proiezione", "No, grazie",
                this::mostraCreaProiezione,
                null);
    }

    /**
     * Notifica l'avvenuta creazione di una proiezione.
     *
     * @param titoloFilm Il titolo del film.
     * @param data La data della proiezione.
     * @param ora L'ora di inizio.
     */
    public void notificaProiezioneCreata(String titoloFilm, java.time.LocalDate data, String ora) {
        mostraScelta(
                "Proiezione creata con successo!\n\"" + titoloFilm + "\" il " + data + " alle " + ora + ".",
                "Vai alle proiezioni", "Crea un'altra",
                this::mostraGestioneProiezioni,
                null);
    }

    /**
     * Mostra la dashboard delle proiezioni odierne per il bigliettaio.
     */
    public void mostraProiezioniOggi() {
        ProiezioniOggiController oggi = new ProiezioniOggiController(gestoreScene);
        oggi.setUtente(utenteLoggato);
        oggi.inizializza();
        impostaContenutoCentrale(oggi.getRoot());
    }

    /**
     * Apre la vista dedicata alla scansione o verifica manuale dei biglietti.
     */
    public void mostraVerificaBiglietto() {
        VerificaBigliettoController verifica = new VerificaBigliettoController(gestoreScene);
        verifica.setUtente(utenteLoggato);
        verifica.inizializza();
        impostaContenutoCentrale(verifica.getRoot());
    }

    /**
     * Apre il pannello delle impostazioni.
     */
    public void mostraImpostazioni() {
        ImpostazioniController impostazioni = new ImpostazioniController(gestoreScene);
        impostazioni.setUtente(utenteLoggato);
        impostazioni.inizializza();
        impostaContenutoCentrale(impostazioni.getRoot());
    }

    /**
     * Sceglie la dashboard iniziale a seconda del ruolo utente loggato.
     *
     * @param titoloInizialeGuest Stringa precompilata per la ricerca (se presente).
     */
    public void mostraDashboardIniziale(String titoloInizialeGuest) {
        if (!isGuest && utenteLoggato.getRuolo() == cinemax.common.model.Ruolo.BIGLIETTAIO) {
            mostraProiezioniOggi();
        } else {
            mostraDashboardRicercaConTitolo(titoloInizialeGuest);
        }
    }

    /**
     * Carica la dashboard di ricerca globale proiezioni.
     */
    public void mostraDashboardRicerca() {
        mostraDashboardRicercaConTitolo(null);
    }

    /**
     * Carica la dashboard di ricerca preimpostando un titolo.
     *
     * @param titoloInizialeGuest Il titolo da cercare in partenza.
     */
    private void mostraDashboardRicercaConTitolo(String titoloInizialeGuest) {
        DashboardClienteController dashboard =
                new DashboardClienteController(gestoreScene, this, titoloInizialeGuest);
        dashboard.setUtente(utenteLoggato);
        dashboard.inizializza();
        impostaContenutoCentrale(dashboard.getRoot());
    }

    /**
     * Apre la schermata con lo storico delle prenotazioni dell'utente.
     */
    public void mostraMiePrenotazioni() {
        MiePrenotazioniController prenotazioni = new MiePrenotazioniController(gestoreScene, this);
        prenotazioni.setUtente(utenteLoggato);
        prenotazioni.inizializza();
        impostaContenutoCentrale(prenotazioni.getRoot());
    }

    /**
     * Mostra la schermata per creare una prenotazione per una specifica proiezione.
     *
     * @param proiezione La proiezione selezionata.
     */
    public void mostraPrenotazione(Proiezione proiezione) {
        PrenotazioneController prenotazione =
                new PrenotazioneController(gestoreScene, this, proiezione);
        prenotazione.setUtente(utenteLoggato);
        prenotazione.inizializza();
        impostaContenutoCentrale(prenotazione.getRoot());
    }

    /**
     * Mostra l'editor di una singola proiezione esistente.
     *
     * @param proiezione La proiezione da modificare.
     */
    public void mostraModificaProiezione(Proiezione proiezione) {
        ModificaProiezioneController modifica =
                new ModificaProiezioneController(gestoreScene, this, proiezione);
        modifica.setUtente(utenteLoggato);
        modifica.inizializza();
        impostaContenutoCentrale(modifica.getRoot());
    }

    /**
     * Apre la vista per spostare/modificare i dati di una prenotazione.
     *
     * @param prenotazione La prenotazione da manipolare.
     */
    public void mostraModificaPrenotazione(Prenotazione prenotazione) {
        cinemax.client.controller.cliente.ModificaPrenotazioneController modifica =
                new cinemax.client.controller.cliente.ModificaPrenotazioneController(
                        gestoreScene, this, prenotazione);
        modifica.setUtente(utenteLoggato);
        modifica.inizializza();
        impostaContenutoCentrale(modifica.getRoot());
    }

    /**
     * Gestisce il click sul bottone di Autenticazione (Log In / Log Out).
     *
     * @param e L'evento originato dal click.
     */
    private void onAuthCliccato(ActionEvent e) {
        if (isGuest) {
            gestoreScene.vaiALogin();
        } else {
            mostraConferma("Stai per essere disconnesso, sei sicuro?", () -> {
                this.utenteLoggato = null;
                gestoreScene.vaiAStart();
            });
        }
    }
}