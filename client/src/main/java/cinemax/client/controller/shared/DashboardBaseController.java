package cinemax.client.controller.shared;

import cinemax.common.model.Utente;
import javafx.scene.Parent;

/*
 Classe base per le dashboard caricate nell'area centrale del BaseLayoutController.

 Ogni dashboard concreta (Cliente, Proiezionista, Bigliettaio) costruisce la propria
 vista in codice Java e la espone tramite getRoot(). Il layout ospitante la inserisce
 con impostaContenutoCentrale(dashboard.getRoot()).

 Il campo utenteLoggato può essere null: in quel caso la dashboard è in modalità
 Guest (utente non autenticato). Le dashboard non devono nascondere da sole le parti
 riservate: espongono i nodi sensibili (es. i bottoni "Prenota") al layout, che li
 attenua e li blocca per il Guest tramite registraNodoRiservato(...).
 */
public abstract class DashboardBaseController {

    protected Utente utenteLoggato;

    // Imposta il contesto utente (null = Guest). Da chiamare prima di inizializza().
    public void setUtente(Utente utente) {
        this.utenteLoggato = utente;
    }

    // Vero se la dashboard sta operando in modalità Guest (nessun utente autenticato).
    protected boolean isGuest() {
        return utenteLoggato == null;
    }

    // Nodo radice della dashboard, da inserire nell'area centrale del layout.
    public abstract Parent getRoot();

    // Costruzione/preparazione iniziale della vista.
    public abstract void inizializza();

    // Ricarica i dati mostrati (es. dopo una ricerca o un cambio di stato).
    public abstract void aggiornaDati();
}
