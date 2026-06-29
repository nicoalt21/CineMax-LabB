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
        LogServer.richiesta("Proiezioni", "cercaProiezioni");
        try {
            List<Proiezione> lista = proiezioneDAO.cercaProiezione(criteri);
            LogServer.esito("Proiezioni", "cercaProiezioni OK trovate=" + (lista == null ? 0 : lista.size()));
            return lista;
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "cercaProiezioni ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante la ricerca proiezioni", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Proiezione visualizzaProiezione(LocalDateTime dataOra) throws RemoteException {
        LogServer.richiesta("Proiezioni", "visualizzaProiezione dataOra=" + dataOra);
        try {
            return proiezioneDAO.ottieniProiezione(dataOra);
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "visualizzaProiezione ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante la visualizzazione proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean aggiungiProiezione(Proiezione proiezione) throws RemoteException {
        LogServer.richiesta("Proiezioni", "aggiungiProiezione film="
                + (proiezione != null && proiezione.getFilm() != null ? proiezione.getFilm().getTitolo() : "?")
                + " dataOra=" + (proiezione != null ? proiezione.getDataOra() : "?"));
        try {
            boolean ok = proiezioneDAO.aggiungiProiezione(proiezione);
            LogServer.esito("Proiezioni", "aggiungiProiezione " + (ok ? "OK" : "RIFIUTATA (sovrapposizione)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "aggiungiProiezione ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante l'aggiunta proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean modificaProiezione(LocalDateTime dataOraAttuale, int idFilm,
                                      LocalDateTime nuovaDataOra, double costo) throws RemoteException {
        LogServer.richiesta("Proiezioni", "modificaProiezione attuale=" + dataOraAttuale
                + " idFilm=" + idFilm + " nuovaDataOra=" + nuovaDataOra + " costo=" + costo);
        try {
            boolean ok = proiezioneDAO.modificaProiezione(dataOraAttuale, idFilm, nuovaDataOra, costo);
            LogServer.esito("Proiezioni", "modificaProiezione " + (ok
                    ? "OK" : "RIFIUTATA (prenotazioni esistenti o sovrapposizione)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "modificaProiezione ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante la modifica proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean eliminaProiezione(LocalDateTime dataOra) throws RemoteException {
        LogServer.richiesta("Proiezioni", "eliminaProiezione dataOra=" + dataOra);
        try {
            boolean ok = proiezioneDAO.eliminaProiezione(dataOra);
            LogServer.esito("Proiezioni", "eliminaProiezione " + (ok ? "OK" : "RIFIUTATA (prenotazioni esistenti)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "eliminaProiezione ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante l'eliminazione proiezione", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean aggiungiFilm(Film film) throws RemoteException {
        LogServer.richiesta("Proiezioni", "aggiungiFilm titolo="
                + (film != null ? film.getTitolo() : "?"));
        try {
            boolean ok = filmDAO.aggiungi(film);
            LogServer.esito("Proiezioni", "aggiungiFilm " + (ok ? "OK" : "RIFIUTATA (duplicato)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "aggiungiFilm ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante l'aggiunta film", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Film> elencaFilm() throws RemoteException {
        LogServer.richiesta("Proiezioni", "elencaFilm");
        try {
            return filmDAO.elencaTutti();
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "elencaFilm ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante l'elenco film", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Film> cercaFilmPerTitolo(String titoloParziale) throws RemoteException {
        LogServer.richiesta("Proiezioni", "cercaFilmPerTitolo titolo=" + titoloParziale);
        try {
            return filmDAO.cercaPerTitolo(titoloParziale);
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "cercaFilmPerTitolo ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante la ricerca film", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<LocalTime> finestreLibere(LocalDate giorno, int durataMinuti,
                                          LocalDateTime dataOraProiezioneDaEscludere) throws RemoteException {
        LogServer.richiesta("Proiezioni", "finestreLibere giorno=" + giorno
                + " durata=" + durataMinuti + " escludi=" + dataOraProiezioneDaEscludere);
        try {
            return proiezioneDAO.calcolaFinestreLibere(giorno, durataMinuti, dataOraProiezioneDaEscludere);
        } catch (SQLException e) {
            LogServer.esito("Proiezioni", "finestreLibere ERRORE DB: " + e.getMessage());
            throw new RemoteException("Errore durante il calcolo finestre libere", e);
        }
    }
}