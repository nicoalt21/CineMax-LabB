package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardPrenotazione;
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

/*
 Dashboard "Le mie prenotazioni" del Cliente, costruita interamente in codice Java.

 Mostra le prenotazioni dell'utente loggato divise in due sezioni:
   - Prenotazioni attive (proiezione futura): card con "Annulla prenotazione".
   - Prenotazioni passate (proiezione precedente a oggi): card semi-trasparenti, sola
     consultazione, nessuna azione (la proiezione è già avvenuta).

 I dati arrivano dal servizio remoto (oggi: implementazione finta in memoria) tramite
 il FornitoreServizi ottenuto dal GestoreScene.
 */
public class MiePrenotazioniController extends DashboardBaseController {

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    private final VBox radice = new VBox(15);
    private final VBox contenitoreAttive = new VBox(12);
    private final VBox contenitorePassate = new VBox(12);
    private final Label labelStato = new Label();
    private final Label titoloPassate = new Label("Prenotazioni passate");

    public MiePrenotazioniController(GestoreScene gestoreScene, BaseLayoutController layout) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

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

    // Divide le prenotazioni in attive (future) e passate, e popola le due sezioni.
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

    // Apre l'overlay coi dettagli della prenotazione: codice ben visibile/copiabile e i
    // due bottoni Annulla/Modifica, che delegano agli stessi handler usati dalle card.
    private void mostraDettagli(Prenotazione p) {
        layout.mostraDettagliPrenotazione(p, this::gestisciAnnullamento, this::gestisciModifica);
    }

    // Avvia la modifica/spostamento della prenotazione su un'altra proiezione dello
    // stesso film (schermata dedicata gestita dal layout).
    private void gestisciModifica(Prenotazione p) {
        layout.mostraModificaPrenotazione(p);
    }

    // Chiede conferma in-app e, se confermato, annulla la prenotazione. Il servizio
    // rimuove la prenotazione e restituisce i posti alla proiezione; ricarichiamo per
    // riflettere il cambiamento.
    private void gestisciAnnullamento(Prenotazione p) {
        layout.mostraConferma("Vuoi davvero eliminare questa prenotazione?",
                () -> eseguiAnnullamento(p));
    }

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
