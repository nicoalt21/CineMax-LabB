package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardProiezione;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 Schermata di modifica (spostamento) di una prenotazione, caricata nell'area centrale del
 BaseLayoutController come le altre dashboard del Cliente.

 Il cliente sposta una prenotazione esistente su un'altra proiezione DELLO STESSO FILM. Il
 numero di biglietti non cambia (il server sposta la prenotazione cosi' com'e'), quindi
 mostriamo solo le proiezioni alternative che hanno posti liberi sufficienti per gli stessi
 biglietti, future e diverse da quella attuale.

 In alto un riepilogo della prenotazione corrente; sotto, l'elenco delle proiezioni
 candidate come card cliccabili. Al click su una card si chiede conferma e si invoca
 modificaPrenotazione(codice, nuovaDataOra). Esiti gestiti: spostata (torna a "Le mie
 prenotazioni"), rifiutata dal server (messaggio), server irraggiungibile.

 Costruita interamente in codice Java (niente FXML), in linea con il resto della UI.
 */
public class ModificaPrenotazioneController extends DashboardBaseController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    // Prenotazione da spostare, passata da "Le mie prenotazioni".
    private final Prenotazione prenotazione;

    private final VBox radice = new VBox(16);
    private final VBox contenitoreCandidate = new VBox(12);
    private final Label labelStato = new Label();

    public ModificaPrenotazioneController(GestoreScene gestoreScene,
                                          BaseLayoutController layout,
                                          Prenotazione prenotazione) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
        this.prenotazione = prenotazione;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

    @Override
    public void inizializza() {
        radice.setPadding(new Insets(16));
        radice.getStyleClass().add("sfondo-principale");

        Label titoloSezione = new Label("Sposta prenotazione");
        titoloSezione.getStyleClass().add("titolo-principale");
        titoloSezione.setStyle("-fx-font-size: 26px;");

        // Riepilogo della proiezione attualmente prenotata (sola lettura: nessuna azione
        // registrata sulla card, quindi i suoi bottoni restano nascosti).
        Proiezione attuale = prenotazione.getProiezione();
        Label sottotitolo = new Label("Stai spostando "
                + prenotazione.getNumeroBiglietti()
                + (prenotazione.getNumeroBiglietti() == 1 ? " biglietto" : " biglietti")
                + " per \"" + attuale.getFilm().getTitolo() + "\".");
        sottotitolo.getStyleClass().add("testo-secondario");
        sottotitolo.setWrapText(true);

        CardProiezione riepilogo = new CardProiezione();
        riepilogo.compilaDatiProiezione(attuale, null);

        Label titoloElenco = new Label("Scegli la nuova proiezione");
        titoloElenco.getStyleClass().add("titolo-principale");
        titoloElenco.setStyle("-fx-font-size: 18px;");

        labelStato.getStyleClass().add("testo-secondario");

        ScrollPane scroll = new ScrollPane(contenitoreCandidate);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Barra inferiore con "Annulla": abbandona lo spostamento e torna a "Le mie
        // prenotazioni" senza modificare nulla.
        Button btnAnnulla = new Button("Annulla");
        btnAnnulla.getStyleClass().add("bottone-secondario");
        btnAnnulla.setOnAction(e -> layout.mostraMiePrenotazioniPubblica());
        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);
        HBox barraInferiore = new HBox(8, btnAnnulla, spazio);
        barraInferiore.setAlignment(Pos.CENTER_LEFT);

        radice.getChildren().addAll(titoloSezione, sottotitolo, riepilogo,
                titoloElenco, labelStato, scroll, barraInferiore);

        aggiornaDati();
    }

    @Override
    public void aggiornaDati() {
        labelStato.setText("Caricamento proiezioni disponibili...");
        contenitoreCandidate.getChildren().clear();

        Proiezione attuale = prenotazione.getProiezione();
        int bigliettiNecessari = prenotazione.getNumeroBiglietti();

        // Cerco le proiezioni dello stesso film, da oggi in poi.
        CriteriRicercaProiezione criteri = new CriteriRicercaProiezione();
        criteri.setTitolo(attuale.getFilm().getTitolo());
        criteri.setDataInizio(LocalDate.now());

        try {
            List<Proiezione> trovate = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni()
                    .cercaProiezioni(criteri);
            mostraCandidate(filtraCandidate(trovate, attuale, bigliettiNecessari));
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }

    /*
     Tiene solo le proiezioni adatte come destinazione: stesso film (gia' filtrato dalla
     ricerca per titolo, ma ricontrollo per sicurezza), future, diverse da quella attuale
     e con posti liberi sufficienti per gli stessi biglietti.
    */
    private List<Proiezione> filtraCandidate(List<Proiezione> trovate, Proiezione attuale,
                                             int bigliettiNecessari) {
        List<Proiezione> candidate = new ArrayList<>();
        if (trovate == null) {
            return candidate;
        }
        LocalDateTime adesso = LocalDateTime.now();
        String titoloAttuale = attuale.getFilm().getTitolo();
        for (Proiezione p : trovate) {
            boolean stessoFilm = p.getFilm().getTitolo().equals(titoloAttuale);
            boolean stessaProiezione = p.getDataOra().equals(attuale.getDataOra());
            boolean futura = p.getDataOra().isAfter(adesso);
            boolean postiSufficienti = p.getPostiLiberi() >= bigliettiNecessari;
            if (stessoFilm && !stessaProiezione && futura && postiSufficienti) {
                candidate.add(p);
            }
        }
        return candidate;
    }

    private void mostraCandidate(List<Proiezione> candidate) {
        contenitoreCandidate.getChildren().clear();

        if (candidate.isEmpty()) {
            labelStato.setText("Nessuna altra proiezione disponibile per questo film "
                    + "con posti sufficienti.");
            return;
        }

        labelStato.setText(candidate.size()
                + (candidate.size() == 1 ? " proiezione disponibile." : " proiezioni disponibili."));

        for (Proiezione p : candidate) {
            CardProiezione card = new CardProiezione();
            card.compilaDatiProiezione(p, null); // null: nessun bottone d'azione interno
            // Click o INVIO/SPAZIO sulla card: scelta della nuova proiezione.
            card.setAzioneCard(this::chiediConferma);
            contenitoreCandidate.getChildren().add(card);
        }
    }

    // Chiede conferma in-app riportando la nuova collocazione, poi sposta la prenotazione.
    private void chiediConferma(Proiezione nuova) {
        String titolo = nuova.getFilm().getTitolo();
        String quando = nuova.getDataOra().format(FORMATO_DATA);
        String messaggio = "Spostare la prenotazione su \"" + titolo + "\" del " + quando + "?";
        layout.mostraConferma(messaggio, () -> eseguiSpostamento(nuova));
    }

    // Invia la richiesta di spostamento al servizio remoto e gestisce l'esito.
    private void eseguiSpostamento(Proiezione nuova) {
        labelStato.setText("Spostamento in corso...");
        try {
            boolean ok = gestoreScene.getFornitoreServizi()
                    .getServizioPrenotazioni()
                    .modificaPrenotazione(prenotazione.getCodice(), nuova.getDataOra());
            if (ok) {
                // Spostata: torno a "Le mie prenotazioni", che ricarica i dati aggiornati.
                layout.mostraMiePrenotazioniPubblica();
            } else {
                // Il server ha rifiutato (es. posti non piu' disponibili nel frattempo,
                // o vincoli temporali). Ricarico le candidate per riflettere lo stato.
                labelStato.setText("Spostamento non riuscito: la proiezione scelta non e' "
                        + "piu' disponibile. Scegline un'altra.");
                aggiornaDati();
            }
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }
}
