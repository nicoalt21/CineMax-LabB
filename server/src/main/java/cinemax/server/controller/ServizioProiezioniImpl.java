package cinemax.server.controller;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import cinemax.common.remote.ServizioProiezioni;
import cinemax.server.persistence.FilmDAO;
import cinemax.server.persistence.ProiezioneDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementazione RMI del servizio di gestione delle proiezioni.
 */
public class ServizioProiezioniImpl extends UnicastRemoteObject implements ServizioProiezioni {

    private final ProiezioneDAO proiezioneDAO;
    private final FilmDAO filmDAO;

    /**
     * Costruttore. Richiesto da UnicastRemoteObject.
     *
     * @throws RemoteException in caso di errore RMI
     */
    public ServizioProiezioniImpl() throws RemoteException {
        super();
        this.proiezioneDAO = new ProiezioneDAO();
        this.filmDAO = new FilmDAO();
    }

    /** {@inheritDoc} */
    @Override
    public List<Proiezione> cercaProiezioni(CriteriRicercaProiezione criteri) throws RemoteException {
        try {
            return proiezioneDAO.cercaProiezione(criteri);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca proiezioni", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Proiezione visualizzaProiezione(LocalDateTime dataOra) throws RemoteException {
        try {
            return proiezioneDAO.ottieniProiezione(dataOra);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la visualizzazione proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean aggiungiProiezione(Proiezione proiezione) throws RemoteException {
        try {
            return proiezioneDAO.aggiungiProiezione(proiezione);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante l'aggiunta proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean modificaProiezione(LocalDateTime dataOraAttuale, int idFilm,
                                      LocalDateTime nuovaDataOra, double costo) throws RemoteException {
        try {
            return proiezioneDAO.modificaProiezione(dataOraAttuale, idFilm, nuovaDataOra, costo);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la modifica proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean eliminaProiezione(LocalDateTime dataOra) throws RemoteException {
        try {
            return proiezioneDAO.eliminaProiezione(dataOra);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante l'eliminazione proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean aggiungiFilm(Film film) throws RemoteException {
        try {
            return filmDAO.aggiungi(film);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante l'aggiunta film", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Film> elencaFilm() throws RemoteException {
        try {
            return filmDAO.elencaTutti();
        } catch (SQLException e) {
            throw new RemoteException("Errore durante l'elenco film", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Film> cercaFilmPerTitolo(String titoloParziale) throws RemoteException {
        try {
            return filmDAO.cercaPerTitolo(titoloParziale);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca film", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<LocalTime> finestreLibere(LocalDate giorno, int durataMinuti,
                                          LocalDateTime dataOraProiezioneDaEscludere) throws RemoteException {
        try {
            return proiezioneDAO.calcolaFinestreLibere(giorno, durataMinuti, dataOraProiezioneDaEscludere);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il calcolo finestre libere", e);
        }
    }
}