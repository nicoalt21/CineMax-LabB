/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardProiezione;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;

/*
 Schermata di prenotazione di una proiezione, costruita interamente in codice Java
 (niente FXML, come il resto della UI) e caricata nell'area centrale del
 BaseLayoutController, esattamente come le altre dashboard del Cliente.

 Flusso:
   1. la dashboard di ricerca chiama BaseLayoutController.mostraPrenotazione(proiezione);
   2. qui mostriamo il riepilogo della proiezione (riusando CardProiezione in sola
      lettura), un selettore del numero di biglietti (1..posti liberi) e il totale
      aggiornato in tempo reale;
   3. alla conferma si chiede conferma in-app e si invoca creaPrenotazione sul servizio
      remoto; in caso di successo si mostra il codice generato dal server e si va a
      "Le mie prenotazioni"; in caso di fallimento si resta qui con un messaggio.

 Controlli di sicurezza lato client (la verifica forte resta sul server):
   - solo clienti registrati: il Guest non arriva qui perché il bottone "Prenota" è
     bloccato dal layout; in più, per difesa, ricontrolliamo isGuest();
   - limite d'età del film tramite FasciaEta;
   - numero biglietti compreso tra 1 e i posti liberi.
 */
public class PrenotazioneController extends DashboardBaseController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    // Proiezione da prenotare, passata dalla dashboard di ricerca.
    private final Proiezione proiezione;

    private final VBox radice = new VBox(18);
    private final Label labelStato = new Label();
    private final Label labelTotale = new Label();
    private final Button btnConferma = new Button("Conferma prenotazione");

    // Selettore del numero di biglietti (1..posti liberi). Costruito in inizializza()
    // perché il range dipende dai posti liberi della proiezione.
    private Spinner<Integer> selettoreBiglietti;

    public PrenotazioneController(GestoreScene gestoreScene,
                                  BaseLayoutController layout,
                                  Proiezione proiezione) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
        this.proiezione = proiezione;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titoloSezione = new Label("Prenota proiezione");
        titoloSezione.getStyleClass().add("titolo-principale");
        titoloSezione.setStyle("-fx-font-size: 26px;");

        labelStato.getStyleClass().add("testo-secondario");

        // Riepilogo della proiezione: riuso CardProiezione in sola lettura (senza
        // registrare azioni, quindi i suoi bottoni restano nascosti).
        CardProiezione riepilogo = new CardProiezione();
        riepilogo.compilaDatiProiezione(proiezione, null);

        radice.getChildren().addAll(titoloSezione, labelStato, riepilogo);

        // Caso limite: proiezione esaurita. Mostriamo il messaggio e non costruiamo il
        // selettore: l'utente può solo tornare indietro.
        int postiLiberi = proiezione.getPostiLiberi();
        if (postiLiberi <= 0) {
            labelStato.setText("Spiacenti, questa proiezione è esaurita.");
            radice.getChildren().add(costruisciBarraInferiore(false));
            return;
        }

        // Blocco età: se l'utente è troppo giovane, niente prenotazione.
        if (!isGuest()) {
            int etaMinima = proiezione.getFilm().getEtaMinima();
            if (!FasciaEta.puoPrenotare(utenteLoggato.getDataNascita(), etaMinima)) {
                labelStato.setText("Non puoi prenotare: il film è vietato ai minori di "
                        + etaMinima + " anni.");
                radice.getChildren().add(costruisciBarraInferiore(false));
                return;
            }
        }

        radice.getChildren().add(costruisciSelettore(postiLiberi));
        radice.getChildren().add(costruisciBarraInferiore(true));

        aggiornaDati();
    }

    @Override
    public void aggiornaDati() {
        aggiornaTotale();
    }

    // Riga "Numero di biglietti" con lo Spinner (1..posti liberi).
    private VBox costruisciSelettore(int postiLiberi) {
        Label etichetta = new Label("Numero di biglietti");
        etichetta.getStyleClass().add("etichetta-campo");

        selettoreBiglietti = new Spinner<>();
        SpinnerValueFactory<Integer> valori =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, postiLiberi, 1);
        selettoreBiglietti.setValueFactory(valori);
        selettoreBiglietti.setEditable(false);
        selettoreBiglietti.setMaxWidth(140);
        // Ogni cambio aggiorna il totale mostrato.
        selettoreBiglietti.valueProperty().addListener((obs, vecchio, nuovo) -> aggiornaTotale());

        labelTotale.getStyleClass().add("titolo-principale");
        labelTotale.setStyle("-fx-font-size: 18px;");

        VBox blocco = new VBox(8, etichetta, selettoreBiglietti, labelTotale);
        blocco.setAlignment(Pos.CENTER_LEFT);
        return blocco;
    }

    // Barra inferiore con "Indietro" e (se prenotabile) "Conferma prenotazione".
    private HBox costruisciBarraInferiore(boolean prenotabile) {
        Button btnIndietro = new Button("Indietro");
        btnIndietro.getStyleClass().add("bottone-secondario");
        btnIndietro.setOnAction(e -> layout.mostraDashboardRicercaPubblica());

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);

        HBox barra = new HBox(8, btnIndietro, spazio);
        barra.setAlignment(Pos.CENTER_LEFT);

        if (prenotabile) {
            btnConferma.getStyleClass().add("bottone-primario");
            btnConferma.setOnAction(e -> chiediConferma());
            barra.getChildren().add(btnConferma);
        }
        return barra;
    }

    // Ricalcola il totale (numero biglietti * costo) e lo mostra.
    private void aggiornaTotale() {
        if (selettoreBiglietti == null) {
            return;
        }
        int quantita = selettoreBiglietti.getValue();
        double totale = quantita * proiezione.getCostoBiglietto();
        labelTotale.setText(String.format("Totale: %.2f €", totale));
    }

    // Chiede conferma in-app, riportando i dati salienti, poi effettua la prenotazione.
    private void chiediConferma() {
        int quantita = selettoreBiglietti.getValue();
        String titolo = proiezione.getFilm().getTitolo();
        String quando = proiezione.getDataOra().format(FORMATO_DATA);
        double totale = quantita * proiezione.getCostoBiglietto();

        String messaggio = String.format(
                "Confermi la prenotazione di %d bigliett%s per \"%s\" del %s?%nTotale: %.2f €",
                quantita, (quantita == 1 ? "o" : "i"), titolo, quando, totale);

        layout.mostraConferma(messaggio, this::eseguiPrenotazione);
    }

    // Invia la richiesta di prenotazione al servizio remoto e gestisce l'esito.
    private void eseguiPrenotazione() {
        // Difesa: il Guest non dovrebbe poter arrivare qui (bottone bloccato dal layout).
        if (isGuest()) {
            labelStato.setText("Devi effettuare l'accesso per prenotare.");
            return;
        }

        int quantita = selettoreBiglietti.getValue();

        // Disabilito il bottone durante la richiesta per evitare doppie prenotazioni.
        btnConferma.setDisable(true);
        labelStato.setText("Prenotazione in corso...");

        try {
            Prenotazione creata = gestoreScene.getFornitoreServizi()
                    .getServizioPrenotazioni()
                    .creaPrenotazione(
                            proiezione.getDataOra(),
                            utenteLoggato.getUsername(),
                            quantita);

            if (creata == null) {
                // Il server ha rifiutato (es. posti non più disponibili nel frattempo).
                btnConferma.setDisable(false);
                labelStato.setText("Prenotazione non riuscita: posti non più disponibili. "
                        + "Aggiorna la ricerca e riprova.");
                return;
            }

            // Successo: mostro il codice generato dal server e porto l'utente alle sue
            // prenotazioni. Uso l'overlay del layout come semplice avviso (un solo bottone
            // utile: "Vai alle mie prenotazioni").
            String codice = creata.getCodice();
            layout.mostraScelta(
                    "Prenotazione confermata!\nCodice: " + codice,
                    "Le mie prenotazioni", "Chiudi",
                    layout::mostraMiePrenotazioniPubblica,
                    layout::mostraDashboardRicercaPubblica);

        } catch (RemoteException e) {
            btnConferma.setDisable(false);
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }
}
