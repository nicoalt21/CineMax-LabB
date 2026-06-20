package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardProiezione;
import cinemax.client.gui.component.FilterBarComponent;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/*
 Dashboard del Cliente, riutilizzata anche per il Guest (utente non autenticato).

 Una sola classe serve entrambi i casi: la differenza tra Cliente e Guest NON è gestita
 qui dentro, ma dal BaseLayoutController. Questa dashboard costruisce le card e, per ogni
 azione riservata (il bottone "Prenota"), la registra presso il layout con
 registraNodoRiservato(...): sarà il layout ad attenuarla e bloccarla per il Guest.

 Funzionalità coperte (vedi specifiche, sezione Guest / Cliente):
   - barra di ricerca proiezioni (titolo, genere, intervallo date, intervallo prezzo)
   - elenco risultati come card
   - apertura dettagli proiezione (login non necessario)
   - prenotazione: visibile a tutti, ma cliccabile solo dai clienti registrati

 Costruita interamente in codice Java (niente FXML), in linea con il resto della UI.
 */
public class DashboardClienteController extends DashboardBaseController {

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    private final VBox radice = new VBox(15);
    private final FilterBarComponent barraFiltri = new FilterBarComponent();
    private final VBox contenitoreRisultati = new VBox(12);
    private final Label labelStato = new Label();

    // Titolo eventualmente indicato dall'utente Guest nel menu iniziale (puo' essere null).
    private final String titoloInizialeGuest;

    public DashboardClienteController(GestoreScene gestoreScene,
                                      BaseLayoutController layout,
                                      String titoloInizialeGuest) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
        this.titoloInizialeGuest = titoloInizialeGuest;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titoloSezione = new Label(isGuest() ? "Esplora le proiezioni" : "Cerca e prenota");
        titoloSezione.getStyleClass().add("titolo-principale");
        titoloSezione.setStyle("-fx-font-size: 26px;");

        // La barra mostra tutti i filtri (date + prezzo) per la ricerca proiezioni.
        barraFiltri.impostaVisibilitaFiltri(true, true);
        barraFiltri.setListenerRicerca(this::eseguiRicerca);

        // Se il Guest è arrivato indicando un titolo, lo precompilo nella barra.
        if (titoloInizialeGuest != null && !titoloInizialeGuest.isBlank()) {
            barraFiltri.impostaFiltriPredefiniti(titoloInizialeGuest, null);
        }

        labelStato.getStyleClass().add("testo-secondario");

        ScrollPane scroll = new ScrollPane(contenitoreRisultati);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titoloSezione, barraFiltri, labelStato, scroll);

        // Caricamento iniziale: per il Guest con titolo indicato mostro subito le sue
        // proiezioni; altrimenti mostro un elenco/placeholder.
        aggiornaDati();
    }

    @Override
    public void aggiornaDati() {
        if (titoloInizialeGuest != null && !titoloInizialeGuest.isBlank()) {
            // Guest entrato indicando un film: ricerca automatica nei prossimi 3 mesi.
            CriteriRicercaProiezione criteriGuest = new CriteriRicercaProiezione();
            criteriGuest.setTitolo(titoloInizialeGuest);
            criteriGuest.setDataInizio(LocalDate.now());
            criteriGuest.setDataFine(LocalDate.now().plusMonths(3));
            eseguiRicerca(criteriGuest);
        } else {
            caricaProiezioniIniziali();
        }
    }

    // Caricamento dell'elenco di partenza (senza filtri specifici).
    private void caricaProiezioniIniziali() {
        labelStato.setText("Caricamento proiezioni...");

        // RICHIESTA AL SERVER
        // Input:  CriteriRicercaProiezione "vuoto" (nessun filtro) -> proiezioni future.
        // Output: List<Proiezione> (se null o vuota, nessun risultato).
        // Esempio:
        //   List<Proiezione> risultati = connettoreServer.cercaProiezione(new CriteriRicercaProiezione());
        List<Proiezione> risultati = null; // placeholder finchè non c'è il backend

        mostraRisultati(risultati);
    }

    // Invocata dalla barra filtri quando l'utente preme "Cerca".
    private void eseguiRicerca(CriteriRicercaProiezione criteri) {
        labelStato.setText("Ricerca in corso...");

        // RICHIESTA AL SERVER
        // Input:  CriteriRicercaProiezione (criteri scelti dall'utente).
        // Output: List<Proiezione> risultati (se null, nessun riscontro).
        // Esempio:
        //   List<Proiezione> risultati = connettoreServer.cercaProiezione(criteri);
        List<Proiezione> risultati = null; // placeholder finchè non c'è il backend

        mostraRisultati(risultati);
    }

    // Costruisce le card a partire dai risultati e aggiorna l'area centrale.
    private void mostraRisultati(List<Proiezione> risultati) {
        contenitoreRisultati.getChildren().clear();

        if (risultati == null || risultati.isEmpty()) {
            labelStato.setText("Nessuna proiezione trovata.");
            return;
        }

        labelStato.setText(risultati.size() + " proiezioni trovate.");

        // Ruolo dell'utente: null per il Guest (la card usa l'etichetta di default).
        Ruolo ruolo = isGuest() ? null : utenteLoggato.getRuolo();

        for (Proiezione p : risultati) {
            CardProiezione card = new CardProiezione();
            card.compilaDatiProiezione(p, ruolo);

            // Click sull'intera card -> dettagli proiezione (consentito anche al Guest).
            card.setOnMouseClicked(e -> mostraDettagliProiezione(p));

            // Azione "Prenota": disponibile a tutti come bottone, ma il layout la blocca
            // per il Guest. La registro come nodo riservato.
            card.setAzionePrincipale(this::avviaPrenotazione);
            layout.registraNodoRiservato(card.getBottonePrincipale());

            contenitoreRisultati.getChildren().add(card);
        }
    }

    // Apertura della schermata di dettaglio di una proiezione (login non necessario).
    private void mostraDettagliProiezione(Proiezione p) {
        // NAVIGAZIONE
        // Passo la proiezione selezionata alla schermata di dettaglio.
        // L'utente corrente (puo' essere null = Guest) serve alla schermata per decidere
        // se mostrare attivo o bloccato il bottone "Prenota".
        // Esempio (quando la navigazione ai dettagli sarà pronta):
        //   gestoreScene.vaiADettagliProiezione(p, utenteLoggato);
    }

    // Avvio del flusso di prenotazione (solo clienti registrati; per il Guest il bottone
    // è bloccato dal layout, quindi questo metodo non viene raggiunto da un Guest).
    private void avviaPrenotazione(Proiezione p) {
        // NAVIGAZIONE / RICHIESTA AL SERVER
        // Apre la schermata di inserimento prenotazione per la proiezione scelta.
        // La creazione vera e propria (creaPrenotazione) avverrà in quella schermata:
        //   Input:  Prenotazione (proiezione + numero biglietti + cliente)
        //   Output: codice prenotazione univoco generato dal server.
    }
}
