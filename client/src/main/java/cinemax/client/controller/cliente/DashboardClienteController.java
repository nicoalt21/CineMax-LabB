package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardProiezione;
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
        // Criteri "vuoti" ma limitati alle proiezioni da oggi in poi.
        CriteriRicercaProiezione criteri = new CriteriRicercaProiezione();
        criteri.setDataInizio(LocalDate.now());
        eseguiRicerca(criteri);
    }

    // Invocata dalla barra filtri quando l'utente preme "Cerca".
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
            contenitoreRisultati.getChildren().clear();
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }

    // Costruisce la riga di bottoni per ordinare i risultati. Ogni bottone ripreme:
    // primo click ordina crescente, secondo click inverte in decrescente.
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

    // Crea un bottone di ordinamento. Cliccandolo si applica il comparatore; se era già
    // attivo, si inverte il verso (crescente <-> decrescente).
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

    // Applica l'ordinamento all'ultima lista e torna alla prima pagina. Il rendering
    // effettivo delle card avviene in renderPagina().
    private void mostraRisultati() {
        if (ultimiRisultati == null || ultimiRisultati.isEmpty()) {
            contenitoreRisultati.getChildren().clear();
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

    // Mostra le card della sola pagina corrente e aggiorna la barra di paginazione.
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

            // Click sull'intera card -> dettagli proiezione (consentito anche al Guest).
            card.setOnMouseClicked(e -> mostraDettagliProiezione(p));

            if (!isGuest() && ruolo == Ruolo.PROIEZIONISTA) {
                // PROIEZIONISTA: il bottone principale è "Modifica" e apre la schermata di
                // modifica della proiezione (NON la prenotazione). Non è un nodo riservato
                // (il proiezionista è autenticato) e non c'è blocco età.
                card.setAzionePrincipale(this::avviaModifica);
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
        // Apre la schermata di inserimento prenotazione per la proiezione scelta.
        // La creazione vera e propria (creaPrenotazione) avviene in quella schermata.
        layout.mostraPrenotazione(p);
    }

    // Avvio del flusso di modifica (solo proiezionista). Apre la schermata di modifica
    // della proiezione, che rispetta i vincoli di integrità lato server.
    private void avviaModifica(Proiezione p) {
        layout.mostraModificaProiezione(p);
    }
}
