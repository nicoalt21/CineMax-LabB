package cinemax.client.controller.bigliettaio;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardProiezione;
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

/*
 Schermata "Proiezioni di oggi" del Bigliettaio, costruita interamente in codice Java.

 Mostra le proiezioni la cui data coincide con la data odierna, come elenco di card in
 sola consultazione (nessuna azione di prenotazione/modifica: il bigliettaio le guarda
 soltanto). I dati arrivano dal servizio remoto (oggi: implementazione finta).
 */
public class ProiezioniOggiController extends DashboardBaseController {

    private final GestoreScene gestoreScene;

    private final VBox radice = new VBox(15);
    private final VBox contenitoreRisultati = new VBox(12);
    private final Label labelStato = new Label();

    public ProiezioniOggiController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

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
