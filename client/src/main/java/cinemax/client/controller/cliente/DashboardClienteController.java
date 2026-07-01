package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.card.CardProiezione;
import cinemax.client.gui.component.BarraPaginazione;
import cinemax.client.gui.component.FilterBarComponent;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dashboard del Cliente, riutilizzata anche per il Guest (utente non autenticato).
 * <p>
 * Una sola classe serve entrambi i casi: la differenza tra Cliente e Guest NON è gestita
 * qui dentro, ma dal BaseLayoutController. Questa dashboard costruisce le card e, per ogni
 * azione riservata (il bottone "Prenota"), la registra presso il layout con
 * registraNodoRiservato(...): sarà il layout ad attenuarla e bloccarla per il Guest.
 * <p>
 * Funzionalità coperte (vedi specifiche, sezione Guest / Cliente):
 * <ul>
 * <li>barra di ricerca proiezioni (titolo, genere, intervallo date, intervallo prezzo)</li>
 * <li>elenco risultati come card</li>
 * <li>apertura dettagli proiezione (login non necessario)</li>
 * <li>prenotazione: visibile a tutti, ma cliccabile solo dai clienti registrati</li>
 * </ul>
 * <p>
 * Costruita interamente in codice Java (niente FXML), in linea con il resto della UI.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class DashboardClienteController extends DashboardBaseController {

    private final GestoreScene gestoreScene;
    private final BaseLayoutController layout;

    private final VBox radice = new VBox(16);
    private final FilterBarComponent barraFiltri = new FilterBarComponent();
    private final VBox contenitoreRisultati = new VBox(12);
    private final Label labelStato = new Label();
    private final HBox barraOrdinamento = new HBox(8);
    private final BarraPaginazione barraPaginazione = new BarraPaginazione();

    // Pagina attualmente visualizzata (1-based).
    private int paginaCorrente = 1;

    // Ultima lista di proiezioni ricevuta dal server: la teniamo per poterla
    // riordinare localmente senza rifare la richiesta.
    private List<Proiezione> ultimiRisultati = new ArrayList<>();

    // Criterio di ordinamento corrente e verso (crescente/decrescente).
    private Comparator<Proiezione> ordinamentoCorrente;
    private boolean ordineCrescente = true;

    // Titolo eventualmente indicato dall'utente Guest nel menu iniziale (puo' essere null).
    private final String titoloInizialeGuest;

    /**
     * Costruisce il controller per la dashboard di esplorazione e ricerca.
     *
     * @param gestoreScene Il gestore delle scene per l'accesso ai servizi.
     * @param layout Il layout di base padre che ospita la dashboard.
     * @param titoloInizialeGuest Il titolo ricercato se la vista è chiamata dopo registrazione.
     */
    public DashboardClienteController(GestoreScene gestoreScene,
                                      BaseLayoutController layout,
                                      String titoloInizialeGuest) {
        this.gestoreScene = gestoreScene;
        this.layout = layout;
        this.titoloInizialeGuest = titoloInizialeGuest;
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
     * Inizializza l'interfaccia visiva e la barra degli strumenti di ricerca.
     */
    @Override
    public void inizializza() {
        radice.setPadding(new Insets(16));
        radice.getStyleClass().add("sfondo-principale");

        // Titolo adattato al ruolo: il proiezionista cerca e modifica, non prenota.
        String testoTitolo;
        if (isGuest()) {
            testoTitolo = "Esplora le proiezioni";
        } else if (utenteLoggato.getRuolo() == Ruolo.PROIEZIONISTA) {
            testoTitolo = "Cerca e modifica";
        } else {
            testoTitolo = "Cerca e prenota";
        }
        Label titoloSezione = new Label(testoTitolo);
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

        costruisciBarraOrdinamento();

        ScrollPane scroll = new ScrollPane(contenitoreRisultati);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Cambio pagina: aggiorno la pagina corrente e ri-renderizzo (nessuna nuova
        // richiesta al server, i dati sono già in ultimiRisultati).
        barraPaginazione.setListenerCambioPagina(pagina -> {
            paginaCorrente = pagina;
            renderPagina();
        });

        radice.getChildren().addAll(titoloSezione, barraFiltri, labelStato, barraOrdinamento, scroll, barraPaginazione);

        // Caricamento iniziale: per il Guest con titolo indicato mostro subito le sue
        // proiezioni; altrimenti mostro un elenco/placeholder.
        aggiornaDati();
    }

    /**
     * Esegue una ricerca iniziale in base allo stato o al titolo di partenza fornito.
     */
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

    /**
     * Esegue la chiamata al server per una ricerca standard (proiezioni da oggi in poi).
     */
    private void caricaProiezioniIniziali() {
        // Criteri "vuoti" ma limitati alle proiezioni da oggi in poi.
        CriteriRicercaProiezione criteri = new CriteriRicercaProiezione();
        criteri.setDataInizio(LocalDate.now());
        eseguiRicerca(criteri);
    }

    /**
     * Interroga il server usando i criteri specificati.
     *
     * @param criteri I filtri in base ai quali recuperare le proiezioni.
     */
    private void eseguiRicerca(CriteriRicercaProiezione criteri) {
        labelStato.setText("Ricerca in corso...");

        // Per cliente e guest non ha senso mostrare proiezioni già avvenute (il dataset
        // contiene anche storico): se l'utente non ha indicato una data "Dal", forziamo
        // il pavimento a oggi. Il proiezionista invece può vedere anche lo storico.
        boolean soloFuture = isGuest() || utenteLoggato.getRuolo() == Ruolo.CLIENTE;
        if (soloFuture && criteri != null && criteri.getDataInizio() == null) {
            criteri.setDataInizio(LocalDate.now());
        }

        // Richiesta al servizio remoto. Se il server non risponde arriva una
        // RemoteException: la segnaliamo nello stato.
        try {
            List<Proiezione> risultati =
                    gestoreScene.getFornitoreServizi()
                            .getServizioProiezioni()
                            .cercaProiezioni(criteri);
            ultimiRisultati = (risultati == null) ? new ArrayList<>() : new ArrayList<>(risultati);
            mostraRisultati();
        } catch (RemoteException e) {
            mostraStatoVuoto("Server non raggiungibile",
                    "Controlla la connessione e riprova la ricerca.");
            labelStato.setText("Server non raggiungibile. Riprova.");
            barraPaginazione.aggiorna(1, 1);
        }
    }

    /**
     * Costruisce la riga di bottoni per ordinare i risultati in locale.
     */
    private void costruisciBarraOrdinamento() {
        barraOrdinamento.setAlignment(Pos.CENTER_LEFT);
        Label etichetta = new Label("Ordina per:");
        etichetta.getStyleClass().add("etichetta-campo");

        Button perNome = bottoneOrdinamento("Nome",
                Comparator.comparing(p -> p.getFilm().getTitolo(), String.CASE_INSENSITIVE_ORDER));
        Button perData = bottoneOrdinamento("Data e ora",
                Comparator.comparing(Proiezione::getDataOra));
        Button perPosti = bottoneOrdinamento("Posti rimanenti",
                Comparator.comparingInt(Proiezione::getPostiLiberi));
        Button perCosto = bottoneOrdinamento("Costo",
                Comparator.comparingDouble(Proiezione::getCostoBiglietto));

        barraOrdinamento.getChildren().addAll(etichetta, perNome, perData, perPosti, perCosto);
    }

    /**
     * Crea un singolo bottone d'ordinamento.
     *
     * @param testo Il testo del bottone.
     * @param comparatore Il comparator usato per riordinare la lista locale.
     * @return L'istanza del Button creato.
     */
    private Button bottoneOrdinamento(String testo, Comparator<Proiezione> comparatore) {
        Button b = new Button(testo);
        b.getStyleClass().add("bottone-ordinamento");
        b.setOnAction(e -> {
            if (ordinamentoCorrente == comparatore) {
                ordineCrescente = !ordineCrescente; // stesso criterio: inverto il verso
            } else {
                ordinamentoCorrente = comparatore;
                ordineCrescente = true;
            }
            // Evidenzio il bottone attivo e aggiorno la freccia del verso.
            for (javafx.scene.Node n : barraOrdinamento.getChildren()) {
                if (n instanceof Button) {
                    n.getStyleClass().remove("bottone-ordinamento-attivo");
                }
            }
            b.getStyleClass().add("bottone-ordinamento-attivo");
            b.setText(testo + (ordineCrescente ? "  ▲" : "  ▼"));
            // Ripulisco le frecce dagli altri bottoni.
            for (javafx.scene.Node n : barraOrdinamento.getChildren()) {
                if (n instanceof Button && n != b) {
                    Button altro = (Button) n;
                    altro.setText(altro.getText().replace("  ▲", "").replace("  ▼", ""));
                }
            }
            mostraRisultati();
        });
        return b;
    }

    /**
     * Applica l'ordinamento (se presente) e resetta la visualizzazione alla prima pagina.
     */
    private void mostraRisultati() {
        if (ultimiRisultati == null || ultimiRisultati.isEmpty()) {
            mostraStatoVuoto("Nessuna proiezione trovata",
                    "Prova a modificare i filtri o ad ampliare l'intervallo di date.");
            labelStato.setText("Nessuna proiezione trovata.");
            barraPaginazione.aggiorna(1, 1); // nasconde la barra
            return;
        }

        // Applico l'ordinamento corrente (se scelto) direttamente su ultimiRisultati,
        // così l'ordine resta coerente fra le pagine.
        if (ordinamentoCorrente != null) {
            Comparator<Proiezione> cmp = ordineCrescente
                    ? ordinamentoCorrente
                    : ordinamentoCorrente.reversed();
            ultimiRisultati.sort(cmp);
        }

        paginaCorrente = 1; // ogni nuova ricerca/ordinamento riparte dalla prima pagina
        renderPagina();
    }

    /**
     * Disegna le card relative alla pagina corrente calcolata per la paginazione.
     */
    private void renderPagina() {
        contenitoreRisultati.getChildren().clear();

        // Risultati per pagina dalle impostazioni condivise (modificabili dall'utente).
        int risultatiPerPagina = gestoreScene.getImpostazioni().getRisultatiPerPagina();

        int totale = ultimiRisultati.size();
        int numeroPagine = (int) Math.ceil((double) totale / risultatiPerPagina);
        if (numeroPagine < 1) {
            numeroPagine = 1;
        }
        // Difesa: se per qualche motivo la pagina è fuori range, la riporto nei limiti.
        if (paginaCorrente > numeroPagine) {
            paginaCorrente = numeroPagine;
        }

        int inizio = (paginaCorrente - 1) * risultatiPerPagina;
        int fine = Math.min(inizio + risultatiPerPagina, totale);
        List<Proiezione> pagina = ultimiRisultati.subList(inizio, fine);

        labelStato.setText(totale + " proiezioni trovate.");

        // Ruolo dell'utente: null per il Guest (la card usa l'etichetta di default).
        Ruolo ruolo = isGuest() ? null : utenteLoggato.getRuolo();

        for (Proiezione p : pagina) {
            CardProiezione card = new CardProiezione();
            card.compilaDatiProiezione(p, ruolo);

            // Click o INVIO/SPAZIO sulla card -> dettagli proiezione (anche per il Guest).
            card.setAzioneCard(this::mostraDettagliProiezione);

            if (!isGuest() && ruolo == Ruolo.PROIEZIONISTA) {
                // PROIEZIONISTA: il bottone principale è "Modifica" e apre la schermata di
                // modifica della proiezione (NON la prenotazione). Non è un nodo riservato
                // (il proiezionista è autenticato) e non c'è blocco età.
                card.setAzionePrincipale(this::avviaModifica);
                // Bottone secondario "Elimina": rimuove la proiezione dal palinsesto, previa
                // conferma. Il server rifiuta l'eliminazione se esistono prenotazioni.
                card.setAzioneSecondaria(this::gestisciEliminazione);
            } else {
                // CLIENTE / GUEST: il bottone principale è "Prenota". Disponibile a tutti
                // come bottone, ma il layout lo blocca per il Guest: lo registro come
                // nodo riservato.
                card.setAzionePrincipale(this::avviaPrenotazione);
                layout.registraNodoRiservato(card.getBottonePrincipale());

                // Blocco età: se l'utente è troppo giovane per il film, disabilito Prenota.
                // Per il Guest (utenteLoggato null) l'età non è verificabile: nessun blocco.
                if (!isGuest()) {
                    int etaMinima = p.getFilm().getEtaMinima();
                    if (!FasciaEta.puoPrenotare(utenteLoggato.getDataNascita(), etaMinima)) {
                        card.bloccaPrenotazione("Vietato ai minori di " + etaMinima + " anni");
                    }
                }

                // Blocco proiezioni passate: non ha senso (ed è rifiutato dal server)
                // prenotare una proiezione già iniziata/conclusa. Il dataset contiene
                // anche proiezioni storiche, quindi questo caso è reale.
                if (!p.getDataOra().isAfter(java.time.LocalDateTime.now())) {
                    card.bloccaPrenotazione("Proiezione già avvenuta");
                }
            }

            contenitoreRisultati.getChildren().add(card);
        }

        barraPaginazione.aggiorna(paginaCorrente, numeroPagine);
    }

    /**
     * Mostra l'overlay laterale delegando la chiamata al Layout.
     *
     * @param p La proiezione da visualizzare.
     */
    private void mostraDettagliProiezione(Proiezione p) {
        layout.mostraDettagliProiezione(p);
    }

    /**
     * Mostra uno stato vuoto esplicativo nell'area centrale.
     *
     * @param titolo Il titolo del messaggio.
     * @param sottotitolo Il sottotitolo esplicativo.
     */
    private void mostraStatoVuoto(String titolo, String sottotitolo) {
        contenitoreRisultati.getChildren().clear();

        Label icona = new Label("\uD83C\uDFAC"); // claqueta (emoji cinema)
        icona.getStyleClass().add("stato-vuoto-icona");

        Label titoloLbl = new Label(titolo);
        titoloLbl.getStyleClass().add("stato-vuoto-titolo");
        titoloLbl.setWrapText(true);

        Label sottoLbl = new Label(sottotitolo);
        sottoLbl.getStyleClass().add("stato-vuoto-sottotitolo");
        sottoLbl.setWrapText(true);

        VBox box = new VBox(8, icona, titoloLbl, sottoLbl);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stato-vuoto");
        box.setPadding(new Insets(64, 16, 64, 16));
        box.setMaxWidth(Double.MAX_VALUE);

        contenitoreRisultati.getChildren().add(box);
    }

    /**
     * Invia l'utente verso la schermata di prenotazione per la data proiezione.
     *
     * @param p La proiezione scelta.
     */
    private void avviaPrenotazione(Proiezione p) {
        layout.mostraPrenotazione(p);
    }

    /**
     * Invia l'utente proiezionista verso l'editor di modifica della proiezione.
     *
     * @param p La proiezione scelta per la modifica.
     */
    private void avviaModifica(Proiezione p) {
        layout.mostraModificaProiezione(p);
    }

    /**
     * Inizia l'azione distruttiva di eliminazione interpellando l'utente.
     *
     * @param p La proiezione da eliminare.
     */
    private void gestisciEliminazione(Proiezione p) {
        String titolo = p.getFilm().getTitolo();
        layout.mostraConferma(
                "Vuoi eliminare la proiezione di \"" + titolo + "\"?",
                () -> eseguiEliminazione(p));
    }

    /**
     * Invia la direttiva di eliminazione al server remoto.
     *
     * @param p La proiezione da rimuovere.
     */
    private void eseguiEliminazione(Proiezione p) {
        try {
            boolean eliminata = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni()
                    .eliminaProiezione(p.getDataOra());
            if (eliminata) {
                aggiornaDati(); // ricarica: la proiezione eliminata non comparira' piu'
            } else {
                labelStato.setText("Impossibile eliminare: esistono prenotazioni "
                        + "per questa proiezione.");
            }
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }
}