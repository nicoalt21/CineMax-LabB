package cinemax.client.controller.proiezionista;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.common.model.Proiezione;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class DashboardProiezionistaController extends DashboardBaseController {

    @Override public void inizializza(){};

    @Override public void aggiornaDati(){};

    private void caricaProiezioniPianificate(){};

    private void caricaProiezioniStoriche(){};

    @FXML public void onAggiungiProiezioneCliccato(ActionEvent e){};

    private void gestisciModificaProiezione(Proiezione p){};

    private void gestisciEliminaProiezione(Proiezione p){};

}
