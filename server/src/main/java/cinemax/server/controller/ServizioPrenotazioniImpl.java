package cinemax.server.controller;

import cinemax.common.model.CriteriRicercaPrenotazione;
import cinemax.common.model.Prenotazione;
import cinemax.common.remote.ServizioPrenotazioni;
import cinemax.server.persistence.PrenotazioneDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementazione RMI del servizio di gestione delle prenotazioni.
 */
public class ServizioPrenotazioniImpl extends UnicastRemoteObject implements ServizioPrenotazioni {

    private final PrenotazioneDAO prenotazioneDAO;

    public ServizioPrenotazioniImpl() throws RemoteException {
        super();
        this.prenotazioneDAO = new PrenotazioneDAO();
    }

    @Override
    public Prenotazione creaPrenotazione(LocalDateTime dataOraProiezione,
                                         String usernameCliente,
                                         int numeroBiglietti) throws RemoteException {
        LogServer.richiesta("Prenotazioni", "creaPrenotazione cliente=" + usernameCliente
                + " proiezione=" + dataOraProiezione + " posti=" + numeroBiglietti);
        try {
            Prenotazione creata = prenotazioneDAO.creaPrenotazione(dataOraProiezione, usernameCliente, numeroBiglietti);
            LogServer.esito("Prenotazioni", creata != null
                    ? "creaPrenotazione OK codice=" + creata.getCodice()
                    : "creaPrenotazione RIFIUTATA (posti/eta/proiezione non validi)");
            return creata;
        } catch (SQLException e) {
            LogServer.esito("Prenotazioni", "creaPrenotazione ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la creazione prenotazione", e);
        }
    }

    @Override
    public List<Prenotazione> visualizzaPrenotazioniCliente(String usernameCliente) throws RemoteException {
        LogServer.richiesta("Prenotazioni", "visualizzaPrenotazioniCliente cliente=" + usernameCliente);
        try {
            List<Prenotazione> lista = prenotazioneDAO.trovaPerCliente(usernameCliente);
            LogServer.esito("Prenotazioni", "visualizzaPrenotazioniCliente OK trovate="
                    + (lista == null ? 0 : lista.size()));
            return lista;
        } catch (SQLException e) {
            LogServer.esito("Prenotazioni", "visualizzaPrenotazioniCliente ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la visualizzazione prenotazioni", e);
        }
    }

    @Override
    public boolean modificaPrenotazione(String codicePrenotazione,
                                        LocalDateTime nuovaDataOra) throws RemoteException {
        LogServer.richiesta("Prenotazioni", "modificaPrenotazione codice=" + codicePrenotazione
                + " nuovaDataOra=" + nuovaDataOra);
        try {
            boolean ok = prenotazioneDAO.modificaData(codicePrenotazione, nuovaDataOra);
            LogServer.esito("Prenotazioni", "modificaPrenotazione " + (ok ? "OK" : "RIFIUTATA"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Prenotazioni", "modificaPrenotazione ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la modifica prenotazione", e);
        }
    }

    @Override
    public boolean cancellaPrenotazione(String codicePrenotazione) throws RemoteException {
        LogServer.richiesta("Prenotazioni", "cancellaPrenotazione codice=" + codicePrenotazione);
        try {
            boolean ok = prenotazioneDAO.cancella(codicePrenotazione);
            LogServer.esito("Prenotazioni", "cancellaPrenotazione " + (ok ? "OK" : "RIFIUTATA"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Prenotazioni", "cancellaPrenotazione ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la cancellazione prenotazione", e);
        }
    }

    @Override
    public List<Prenotazione> cercaPrenotazioni(CriteriRicercaPrenotazione criteri) throws RemoteException {
        LogServer.richiesta("Prenotazioni", "cercaPrenotazioni (bigliettaio)");
        try {
            List<Prenotazione> lista = prenotazioneDAO.cerca(criteri);
            LogServer.esito("Prenotazioni", "cercaPrenotazioni OK trovate="
                    + (lista == null ? 0 : lista.size()));
            return lista;
        } catch (SQLException e) {
            LogServer.esito("Prenotazioni", "cercaPrenotazioni ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la ricerca prenotazioni", e);
        }
    }

    @Override
    public List<Prenotazione> visualizzaPrenotazioniOggi() throws RemoteException {
        LogServer.richiesta("Prenotazioni", "visualizzaPrenotazioniOggi (bigliettaio)");
        try {
            List<Prenotazione> lista = prenotazioneDAO.trovaDiOggi();
            LogServer.esito("Prenotazioni", "visualizzaPrenotazioniOggi OK trovate="
                    + (lista == null ? 0 : lista.size()));
            return lista;
        } catch (SQLException e) {
            LogServer.esito("Prenotazioni", "visualizzaPrenotazioniOggi ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la visualizzazione prenotazioni di oggi", e);
        }
    }
}