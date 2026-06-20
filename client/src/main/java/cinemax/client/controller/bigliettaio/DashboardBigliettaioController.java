package cinemax.client.controller.bigliettaio;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.common.model.Prenotazione;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

public class DashboardBigliettaioController extends DashboardBaseController {

    // Radice della dashboard (per ora vuota: da costruire quando si sviluppa la vista).
    private final VBox radice = new VBox();

    @Override public Parent getRoot(){ return radice; }

    @Override public void inizializza(){};

    @Override public void aggiornaDati(){}; // (Carica quelle di oggi)
    //private void eseguiRicerca(CriteriRicercaPrenotazione criteri){};

    private void mostraDettagliPrenotazione(Prenotazione p){};

}
