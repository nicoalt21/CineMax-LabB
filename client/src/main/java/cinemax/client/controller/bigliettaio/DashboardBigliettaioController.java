package cinemax.client.controller.bigliettaio;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.model.FiltriRicerca;
import cinemax.common.model.Prenotazione;

public class DashboardBigliettaioController extends DashboardBaseController {

    @Override public void inizializza(){};

    @Override public void aggiornaDati(){}; // (Carica quelle di oggi)
    private void eseguiRicerca(FiltriRicerca filtri){};

    private void mostraDettagliPrenotazione(Prenotazione p){};

}
