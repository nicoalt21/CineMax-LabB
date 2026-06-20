package cinemax.client.controller.cliente;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.common.model.Prenotazione;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

public class MiePrenotazioniController extends DashboardBaseController {

    // Radice della dashboard (per ora vuota: da costruire quando si sviluppa la vista).
    private final VBox radice = new VBox();

    @Override public Parent getRoot(){ return radice; }

    @Override public void inizializza(){};

    @Override public void aggiornaDati(){};

    private void gestisciModificaData(Prenotazione p){};

    private void gestisciCancellazione(Prenotazione p){};


}
