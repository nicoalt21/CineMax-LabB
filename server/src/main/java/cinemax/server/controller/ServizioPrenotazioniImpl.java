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

    /**
     * Costruttore. Richiesto da UnicastRemoteObject.
     *
     * @throws RemoteException in caso di errore RMI
     */
    public ServizioPrenotazioniImpl() throws RemoteException {
        super();
        this.prenotazioneDAO = new PrenotazioneDAO();
    }

    /** {@inheritDoc} */
    @Override
    public Prenotazione creaPrenotazione(LocalDateTime dataOraProiezione,
                                         String usernameCliente,
                                         int numeroBiglietti) throws RemoteException {
        try {
            return prenotazioneDAO.creaPrenotazione(dataOraProiezione, usernameCliente, numeroBiglietti);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la creazione prenotazione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Prenotazione> visualizzaPrenotazioniCliente(String usernameCliente) throws RemoteException {
        try {
            return prenotazioneDAO.trovaPerCliente(usernameCliente);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la visualizzazione prenotazioni", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean modificaPrenotazione(String codicePrenotazione,
                                        LocalDateTime nuovaDataOra) throws RemoteException {
        try {
            return prenotazioneDAO.modificaData(codicePrenotazione, nuovaDataOra);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la modifica prenotazione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean cancellaPrenotazione(String codicePrenotazione) throws RemoteException {
        try {
            return prenotazioneDAO.cancella(codicePrenotazione);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la cancellazione prenotazione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Prenotazione> cercaPrenotazioni(CriteriRicercaPrenotazione criteri) throws RemoteException {
        try {
            return prenotazioneDAO.cerca(criteri);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca prenotazioni", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Prenotazione> visualizzaPrenotazioniOggi() throws RemoteException {
        try {
            return prenotazioneDAO.trovaDiOggi();
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la visualizzazione prenotazioni di oggi", e);
        }
    }
}