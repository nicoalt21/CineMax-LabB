package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.card.CardPrenotazione;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Prenotazione;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard "Le mie prenotazioni" del Cliente, costruita interamente in codice Java.
 * <p>
 * Mostra le prenotazioni dell'utente loggato divise in due sezioni:
 * <ul>
 * <li>Prenotazioni attive (proiezione futura): card con "Annulla prenotazione".</li>
 * <li>Prenotazioni passate (proiezione precedente a oggi): card semi-trasparenti, sola
 * consultazione, nessuna azione (la proiezione è già avvenuta).</li>
 * </ul>
 * <p>
 * I dati arrivano dal servizio remoto (oggi: implementazione finta in memoria) tramite
 * il FornitoreServizi ottenuto dal GestoreScene.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class MiePrenotazioniController extends DashboardBaseController {

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    private final VBox radice = new VBox(15);
    private final VBox contenitoreAttive = new VBox(12);
    private final VBox contenitorePassate = new VBox(12);
    private final Label labelStato = new Label();
    private final Label titoloPassate = new Label("Prenotazioni passate");

    /**
     * Costruisce il controller della dashboard delle prenotazioni dell'utente.
     *
     * @param gestoreScene Il gestore delle scene per l'accesso ai servizi.
     * @param layout Il BaseLayoutController che ospita la dashboard.
     */
    public MiePrenotazioniController(GestoreScene gestoreScene, BaseLayoutController layout) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
    }

    /**
     * Restituisce il nodo radice dell'interfaccia.
     *
     * @return Il Parent radice.
     */
    @Override
    public Parent getRoot() {
        return radice;
    }

    /**
     * Inizializza i componenti grafici delle due sezioni separate (attive e passate).
     */
    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titoloSezione = new Label("Le mie prenotazioni");
        titoloSezione.getStyleClass().add("titolo-principale");
        titoloSezione.setStyle("-fx-font-size: 26px;");

        labelStato.getStyleClass().add("testo-secondario");

        titoloPassate.getStyleClass().add("titolo-principale");
        titoloPassate.setStyle("-fx-font-size: 18px;");

        // Contenuto scrollabile: prima le attive, poi il titolo "passate" e le passate.
        VBox contenuto = new VBox(15, contenitoreAttive, titoloPassate, contenitorePassate);
        ScrollPane scroll = new ScrollPane(contenuto);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titoloSezione, labelStato, scroll);

        aggiornaDati();
    }

    /**
     * Interroga il server per recuperare le prenotazioni legate all'utente loggato.
     */
    @Override
    public void aggiornaDati() {
        labelStato.setText("Caricamento prenotazioni...");
        contenitoreAttive.getChildren().clear();
        contenitorePassate.getChildren().clear();

        // Senza utente loggato non c'è nulla da mostrare (non dovrebbe accadere: la
        // voce di menu è riservata ai clienti registrati).
        if (utenteLoggato == null) {
            labelStato.setText("Devi effettuare l'accesso per vedere le tue prenotazioni.");
            return;
        }

        try {
            List<Prenotazione> prenotazioni = gestoreScene.getFornitoreServizi()
                    .getServizioPrenotazioni()
                    .visualizzaPrenotazioniCliente(utenteLoggato.getUsername());
            mostraPrenotazioni(prenotazioni);
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }

    /**
     * Dispone le prenotazioni ritornate dal server dividendole tra attuali e storiche.
     *
     * @param prenotazioni La lista totale delle prenotazioni.
     */
    private void mostraPrenotazioni(List<Prenotazione> prenotazioni) {
        contenitoreAttive.getChildren().clear();
        contenitorePassate.getChildren().clear();

        List<Prenotazione> attive = new ArrayList<>();
        List<Prenotazione> passate = new ArrayList<>();
        LocalDateTime adesso = LocalDateTime.now();

        if (prenotazioni != null) {
            for (Prenotazione p : prenotazioni) {
                if (p.getProiezione().getDataOra().isBefore(adesso)) {
                    passate.add(p);
                } else {
                    attive.add(p);
                }
            }
        }

        // Sezione attive: card con "Modifica" e "Annulla", e click sulla card che apre
        // l'overlay coi dettagli (codice ben visibile) e gli stessi due bottoni.
        for (Prenotazione p : attive) {
            CardPrenotazione card = new CardPrenotazione();
            card.compilaDatiPrenotazione(p, false);
            card.setAzioneModifica(this::gestisciModifica);
            card.setAzioneAnnulla(this::gestisciAnnullamento);
            card.setAzioneCard(this::mostraDettagli);
            contenitoreAttive.getChildren().add(card);
        }

        // Sezione passate: card semi-trasparenti, sola consultazione (nessuna azione).
        for (Prenotazione p : passate) {
            CardPrenotazione card = new CardPrenotazione();
            card.compilaDatiPrenotazione(p, true);
            contenitorePassate.getChildren().add(card);
        }

        // Il titolo "Prenotazioni passate" appare solo se ce ne sono.
        boolean cePassate = !passate.isEmpty();
        titoloPassate.setManaged(cePassate);
        titoloPassate.setVisible(cePassate);

        if (attive.isEmpty() && passate.isEmpty()) {
            labelStato.setText("Non hai ancora effettuato prenotazioni.");
        } else {
            labelStato.setText(attive.size() + " attive, " + passate.size() + " passate.");
        }
    }

    /**
     * Apre l'overlay delegato dal layout per mostrare il dettaglio della prenotazione.
     *
     * @param p La prenotazione da visualizzare nel dettaglio.
     */
    private void mostraDettagli(Prenotazione p) {
        layout.mostraDettagliPrenotazione(p, this::gestisciAnnullamento, this::gestisciModifica);
    }

    /**
     * Naviga l'utente alla schermata di spostamento / modifica prenotazione.
     *
     * @param p La prenotazione che il cliente intende modificare.
     */
    private void gestisciModifica(Prenotazione p) {
        layout.mostraModificaPrenotazione(p);
    }

    /**
     * Inizia l'azione distruttiva di cancellazione chiedendo prima la conferma all'utente.
     *
     * @param p La prenotazione da annullare.
     */
    private void gestisciAnnullamento(Prenotazione p) {
        layout.mostraConferma("Vuoi davvero eliminare questa prenotazione?",
                () -> eseguiAnnullamento(p));
    }

    /**
     * Comunica al server l'annullamento della prenotazione e gestisce i tentativi di refresh.
     *
     * @param p La prenotazione di cui richiedere l'annullamento effettivo al server.
     */
    private void eseguiAnnullamento(Prenotazione p) {
        try {
            boolean ok = gestoreScene.getFornitoreServizi()
                    .getServizioPrenotazioni()
                    .cancellaPrenotazione(p.getCodice());
            if (ok) {
                aggiornaDati();
            } else {
                labelStato.setText("Annullamento non riuscito.");
            }
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }
}