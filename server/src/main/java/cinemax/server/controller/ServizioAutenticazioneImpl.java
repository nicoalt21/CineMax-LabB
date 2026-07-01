package cinemax.server.controller;

import cinemax.common.model.Utente;
import cinemax.common.remote.ServizioAutenticazione;
import cinemax.server.persistence.UtenteDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

/**
 * Implementazione RMI del servizio di autenticazione.
 * <p>
 * Gestisce login, registrazione, modifica profilo e logout degli utenti.
 * La password ricevuta da {@link #login} e {@link #registraCliente} e' gia'
 * cifrata lato client con SHA-256 tramite {@code Cifrario}: il server
 * la confronta o la salva direttamente senza ulteriori trasformazioni.
 * </p>
 *
 * @author Alt Niccolo' Jacopo, 762605, VA
 * @author Gerti Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ServizioAutenticazioneImpl extends UnicastRemoteObject implements ServizioAutenticazione {

    private final UtenteDAO utenteDAO;

    /**
     * Costruisce l'implementazione e la esporta come oggetto remoto.
     *
     * @throws RemoteException in caso di errore durante l'esportazione RMI
     */
    public ServizioAutenticazioneImpl() throws RemoteException {
        super();
        this.utenteDAO = new UtenteDAO();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delega l'autenticazione a {@link UtenteDAO#autenticaUtente}.
     * Gli errori di accesso al database vengono convertiti in
     * {@link RemoteException} tramite {@link LogServer#erroreRemoto}
     * per evitare la trasmissione di tipi non presenti nel classpath del client.
     * </p>
     */
    @Override
    public Utente login(String username, String passwordHash) throws RemoteException {
        LogServer.richiesta("Autenticazione", "login username=" + username);
        try {
            Utente u = utenteDAO.autenticaUtente(username, passwordHash);
            LogServer.esito("Autenticazione", u != null
                    ? "login OK ruolo=" + u.getRuolo()
                    : "login FALLITO (credenziali errate)");
            return u;
        } catch (SQLException e) {
            LogServer.esito("Autenticazione", "login ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante il login", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delega la registrazione a {@link UtenteDAO#registraCliente}.
     * Restituisce {@code false} senza lanciare eccezioni se lo username
     * e' gia' presente nel database.
     * Gli errori di accesso al database vengono convertiti in
     * {@link RemoteException} tramite {@link LogServer#erroreRemoto}.
     * </p>
     */
    @Override
    public boolean registraCliente(Utente utente) throws RemoteException {
        LogServer.richiesta("Autenticazione", "registraCliente username="
                + (utente != null ? utente.getUsername() : "null"));
        try {
            boolean ok = utenteDAO.registraCliente(utente);
            LogServer.esito("Autenticazione", "registraCliente "
                    + (ok ? "OK" : "RIFIUTATA (username gia' in uso)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Autenticazione", "registraCliente ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la registrazione", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delega l'aggiornamento a {@link UtenteDAO#aggiorna}.
     * Lo username non e' modificabile: viene usato come chiave di ricerca.
     * Restituisce {@code false} senza lanciare eccezioni se l'utente
     * non viene trovato nel database.
     * Gli errori di accesso al database vengono convertiti in
     * {@link RemoteException} tramite {@link LogServer#erroreRemoto}.
     * </p>
     */
    @Override
    public boolean modificaUtente(Utente utente) throws RemoteException {
        LogServer.richiesta("Autenticazione", "modificaUtente username="
                + (utente != null ? utente.getUsername() : "null"));
        try {
            boolean ok = utenteDAO.aggiorna(utente);
            LogServer.esito("Autenticazione", "modificaUtente "
                    + (ok ? "OK" : "RIFIUTATA (utente non trovato)"));
            return ok;
        } catch (SQLException e) {
            LogServer.esito("Autenticazione", "modificaUtente ERRORE DB: " + e.getMessage());
            throw LogServer.erroreRemoto("Errore durante la modifica utente", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * RMI e' stateless: il server non mantiene sessioni attive.
     * Il logout e' gestito interamente lato client, che scarta
     * il riferimento all'utente autenticato e torna alla schermata iniziale.
     * </p>
     */
    @Override
    public void logout(String username) throws RemoteException {
        LogServer.richiesta("Autenticazione", "logout username=" + username);
    }
}