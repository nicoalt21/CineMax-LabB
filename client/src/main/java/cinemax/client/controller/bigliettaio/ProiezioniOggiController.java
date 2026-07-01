package cinemax.client.controller.bigliettaio;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.card.CardProiezione;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Proiezione;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

/**
 * Schermata "Proiezioni di oggi" del Bigliettaio, costruita interamente in codice Java.
 * <p>
 * Mostra le proiezioni la cui data coincide con la data odierna, come elenco di card in
 * sola consultazione (nessuna azione di prenotazione/modifica: il bigliettaio le guarda
 * soltanto). I dati arrivano dal servizio remoto (oggi: implementazione finta).
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ProiezioniOggiController extends DashboardBaseController {

    private final GestoreScene gestoreScene;

    private final VBox radice = new VBox(15);
    private final VBox contenitoreRisultati = new VBox(12);
    private final Label labelStato = new Label();

    /**
     * Costruisce il controller per la dashboard delle proiezioni odierne.
     *
     * @param gestoreScene Il gestore delle scene utilizzato per la navigazione e l'accesso ai servizi.
     */
    public ProiezioniOggiController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
    }

    /**
     * Restituisce il nodo radice dell'interfaccia grafica.
     *
     * @return Il Parent che contiene la vista.
     */
    @Override
    public Parent getRoot() {
        return radice;
    }

    /**
     * Inizializza i componenti grafici della schermata e avvia il caricamento dei dati.
     */
    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titolo = new Label("Proiezioni di oggi");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        labelStato.getStyleClass().add("testo-secondario");

        ScrollPane scroll = new ScrollPane(contenitoreRisultati);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titolo, labelStato, scroll);

        aggiornaDati();
    }

    /**
     * Aggiorna la lista delle proiezioni richiedendo al server quelle programmate per la data odierna.
     */
    @Override
    public void aggiornaDati() {
        labelStato.setText("Caricamento proiezioni di oggi...");
        contenitoreRisultati.getChildren().clear();

        // Cerco le proiezioni con data odierna: intervallo [oggi, oggi].
        CriteriRicercaProiezione criteri = new CriteriRicercaProiezione();
        criteri.setDataInizio(LocalDate.now());
        criteri.setDataFine(LocalDate.now());

        try {
            List<Proiezione> proiezioni = gestoreScene.getFornitoreServizi()
                    .getServizioProiezioni()
                    .cercaProiezioni(criteri);
            mostraProiezioni(proiezioni);
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }

    /**
     * Popola l'interfaccia con le card relative alle proiezioni fornite.
     *
     * @param proiezioni La lista delle proiezioni di oggi da visualizzare.
     */
    private void mostraProiezioni(List<Proiezione> proiezioni) {
        contenitoreRisultati.getChildren().clear();

        if (proiezioni == null || proiezioni.isEmpty()) {
            labelStato.setText("Nessuna proiezione in programma oggi.");
            return;
        }

        labelStato.setText(proiezioni.size()
                + (proiezioni.size() == 1 ? " proiezione oggi." : " proiezioni oggi."));

        for (Proiezione p : proiezioni) {
            CardProiezione card = new CardProiezione();
            // Ruolo null: sola consultazione, nessun bottone d'azione registrato.
            card.compilaDatiProiezione(p, null);
            contenitoreRisultati.getChildren().add(card);
        }
    }
}