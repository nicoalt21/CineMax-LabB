package cinemax.client.controller.shared;

import cinemax.common.model.Utente;

public abstract class DashboardBaseController {

    protected Utente utenteLoggato;

    public abstract void inizializza();

    public abstract void aggiornaDati();
}
