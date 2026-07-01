package cinemax.server.persistence;

import cinemax.common.model.Ruolo;
import cinemax.common.model.Utente;

import java.sql.*;
import java.time.LocalDate;

/**
 * DAO per la gestione degli utenti nel database.
 * Gestisce le operazioni di autenticazione, registrazione e ricerca degli utenti.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class UtenteDAO {

    /**
     * Registra un nuovo cliente nel database.
     * La password nell'oggetto utente deve essere già hashata lato client.
     *
     * @param utente utente da registrare con password già cifrata
     * @return true se registrato con successo, false se lo username è già in uso
     * @throws SQLException in caso di errore SQL
     */
    public boolean registraCliente(Utente utente) throws SQLException {
        if (esiste(utente.getUsername())) return false;

        String sql = "INSERT INTO utenti (username, nome, cognome, password_hash, " +
                "data_nascita, domicilio, ruolo) VALUES (?, ?, ?, ?, ?, ?, 'CLIENTE')";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utente.getUsername());
            ps.setString(2, utente.getNome());
            ps.setString(3, utente.getCognome());
            ps.setString(4, utente.getPasswordCifrata());
            if (utente.getDataNascita() != null) {
                ps.setDate(5, Date.valueOf(utente.getDataNascita()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, utente.getLuogoDomicilio());
            ps.executeUpdate();
            return true;
        }
    }

    /**
     * Autentica un utente confrontando username e hash della password.
     *
     * @param username     username dell'utente
     * @param passwordHash password già cifrata con SHA-256 lato client
     * @return utente autenticato senza passwordCifrata, null se le credenziali non corrispondono
     * @throws SQLException in caso di errore SQL
     */
    public Utente autenticaUtente(String username, String passwordHash) throws SQLException {
        String sql = "SELECT * FROM utenti WHERE username = ? AND password_hash = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return creaUtente(rs);
                return null;
            }
        }
    }

    /**
     * Recupera un utente per username senza verificarne la password.
     * Usato internamente da PrenotazioneDAO per recuperare i dati del cliente.
     *
     * @param username username da cercare
     * @return utente trovato, null se non esiste
     * @throws SQLException in caso di errore SQL
     */
    public Utente trovaPerId(String username) throws SQLException {
        String sql = "SELECT * FROM utenti WHERE username = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return creaUtente(rs);
                return null;
            }
        }
    }

    /**
     * Verifica se uno username è già presente nel database.
     * Usato da registraCliente per evitare duplicati.
     *
     * @param username username da verificare
     * @return true se lo username esiste già, false altrimenti
     * @throws SQLException in caso di errore SQL
     */
    public boolean esiste(String username) throws SQLException {
        String sql = "SELECT 1 FROM utenti WHERE username = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Trasforma la riga corrente del ResultSet in un oggetto Utente.
     * La passwordCifrata viene impostata a null: non deve mai uscire dal server.
     *
     * @param rs ResultSet posizionato sulla riga corrente
     * @return oggetto Utente popolato con i dati della riga
     * @throws SQLException in caso di errore SQL
     */
    private Utente creaUtente(ResultSet rs) throws SQLException {
        Date sqlDate = rs.getDate("data_nascita");
        LocalDate dataNascita = (sqlDate != null) ? sqlDate.toLocalDate() : null;

        return new Utente(
                rs.getString("nome"),
                rs.getString("cognome"),
                rs.getString("username"),
                null,
                dataNascita,
                rs.getString("domicilio"),
                Ruolo.valueOf(rs.getString("ruolo"))
        );
    }

    /**
     * Aggiorna i dati di un utente esistente nel database.
     *
     * @param utente utente con i dati aggiornati (la password deve essere già cifrata)
     * @return true se l'aggiornamento è andato a buon fine, false se l'utente non esiste
     * @throws SQLException in caso di errore SQL
     */
    public boolean aggiorna(Utente utente) throws SQLException {
        String sql = "UPDATE utenti SET nome = ?, cognome = ?, password_hash = ?, " +
                "data_nascita = ?, domicilio = ? WHERE username = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utente.getNome());
            ps.setString(2, utente.getCognome());
            ps.setString(3, utente.getPasswordCifrata());
            if (utente.getDataNascita() != null) {
                ps.setDate(4, Date.valueOf(utente.getDataNascita()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setString(5, utente.getLuogoDomicilio());
            ps.setString(6, utente.getUsername());
            int righe = ps.executeUpdate();
            return righe > 0;
        }
    }
}