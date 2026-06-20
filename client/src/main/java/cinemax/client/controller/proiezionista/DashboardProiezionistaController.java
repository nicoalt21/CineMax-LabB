package cinemax.client.controller.proiezionista;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.common.model.Proiezione;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

public class DashboardProiezionistaController extends DashboardBaseController {

    // Radice della dashboard (per ora vuota: da costruire quando si sviluppa la vista).
    private final VBox radice = new VBox();

    @Override public Parent getRoot(){ return radice; }

    @Override public void inizializza(){};

    @Override public void aggiornaDati(){};

    private void caricaProiezioniPianificate(){};

    private void caricaProiezioniStoriche(){};

    @FXML public void onAggiungiProiezioneCliccato(ActionEvent e){};

    private void gestisciModificaProiezione(Proiezione p){};

    private void gestisciEliminaProiezione(Proiezione p){};

}
