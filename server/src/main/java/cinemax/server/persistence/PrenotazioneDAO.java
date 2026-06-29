package cinemax.server.persistence;

import cinemax.common.model.CriteriRicercaPrenotazione;
import cinemax.common.model.Film;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import cinemax.common.model.Utente;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO per la gestione delle prenotazioni nel database.
 */
public class PrenotazioneDAO {

    private static final int CAPIENZA_MASSIMA = 200;

    // Crea una prenotazione in modo atomico (transazione con FOR UPDATE per evitare
    // overbooking concorrente). Ritorna la prenotazione creata, o null se non valida.
    public Prenotazione creaPrenotazione(LocalDateTime dataOraProiezione,
                                         String usernameCliente,
                                         int numeroBiglietti) throws SQLException {
        if (numeroBiglietti <= 0) return null;

        UtenteDAO utenteDAO = new UtenteDAO();
        Utente utente = utenteDAO.trovaPerId(usernameCliente);
        if (utente == null) return null;

        String codice = creaPrenotazioneInterna(utente, dataOraProiezione, numeroBiglietti);
        if (codice == null) return null;

        return ottieniPrenotazione(codice);
    }

    private String creaPrenotazioneInterna(Utente utente, LocalDateTime dataOraProiezione,
                                           int numeroBiglietti) throws SQLException {
        try (Connection conn = DBconnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT p.data_ora, f.eta_minima, " +
                        "COALESCE((SELECT SUM(pr.numero_posti) FROM prenotazioni pr " +
                        "WHERE pr.data_ora = p.data_ora), 0) AS posti_occupati " +
                        "FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                        "WHERE p.data_ora = ? FOR UPDATE";

                int postiOccupati;
                int etaMinima;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(dataOraProiezione));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("[PRENOTAZIONE] RIFIUTATA: proiezione inesistente per data_ora="
                                    + dataOraProiezione);
                            return null;
                        }
                        if (!dataOraProiezione.isAfter(LocalDateTime.now())) {
                            System.out.println("[PRENOTAZIONE] RIFIUTATA: proiezione nel passato data_ora="
                                    + dataOraProiezione + " (adesso=" + LocalDateTime.now() + ")");
                            return null;
                        }
                        postiOccupati = rs.getInt("posti_occupati");
                        etaMinima     = rs.getInt("eta_minima");
                    }
                }

                int postiLiberi = CAPIENZA_MASSIMA - postiOccupati;
                if (numeroBiglietti > postiLiberi) {
                    System.out.println("[PRENOTAZIONE] RIFIUTATA: posti insufficienti richiesti="
                            + numeroBiglietti + " liberi=" + postiLiberi);
                    return null;
                }

                int etaCliente = utente.calcolaEta();
                if (etaCliente != -1 && etaCliente < etaMinima) {
                    System.out.println("[PRENOTAZIONE] RIFIUTATA: et\u00e0 cliente=" + etaCliente
                            + " < et\u00e0 minima film=" + etaMinima);
                    return null;
                }

                String codice = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                String insertSql = "INSERT INTO prenotazioni " +
                        "(codice_univoco, username_cliente, data_ora, numero_posti) " +
                        "VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString   (1, codice);
                    ps.setString   (2, utente.getUsername());
                    ps.setTimestamp(3, Timestamp.valueOf(dataOraProiezione));
                    ps.setInt      (4, numeroBiglietti);
                    ps.executeUpdate();
                }

                conn.commit();
                return codice;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Prenotazione ottieniPrenotazione(String codice) throws SQLException {
        String sql = "SELECT pr.codice_univoco, pr.numero_posti, pr.data_creazione, " +
                "u.username, u.nome, u.cognome, u.domicilio, u.data_nascita, u.ruolo, " +
                "p.data_ora, p.prezzo_biglietto, " +
                "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                "(" + CAPIENZA_MASSIMA + " - COALESCE((SELECT SUM(x.numero_posti) FROM prenotazioni x " +
                "WHERE x.data_ora = p.data_ora), 0)) AS posti_liberi " +
                "FROM prenotazioni pr " +
                "JOIN utenti     u ON pr.username_cliente = u.username " +
                "JOIN proiezioni p ON pr.data_ora         = p.data_ora " +
                "JOIN film       f ON p.id_film           = f.id_film " +
                "WHERE pr.codice_univoco = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codice);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return creaPrenotazione(rs);
                return null;
            }
        }
    }

    public List<Prenotazione> trovaPerCliente(String usernameCliente) throws SQLException {
        String sql = "SELECT pr.codice_univoco, pr.numero_posti, pr.data_creazione, " +
                "u.username, u.nome, u.cognome, u.domicilio, u.data_nascita, u.ruolo, " +
                "p.data_ora, p.prezzo_biglietto, " +
                "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                "(" + CAPIENZA_MASSIMA + " - COALESCE((SELECT SUM(x.numero_posti) FROM prenotazioni x " +
                "WHERE x.data_ora = p.data_ora), 0)) AS posti_liberi " +
                "FROM prenotazioni pr " +
                "JOIN utenti     u ON pr.username_cliente = u.username " +
                "JOIN proiezioni p ON pr.data_ora         = p.data_ora " +
                "JOIN film       f ON p.id_film           = f.id_film " +
                "WHERE pr.username_cliente = ? " +
                "ORDER BY p.data_ora";

        List<Prenotazione> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usernameCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) risultati.add(creaPrenotazione(rs));
            }
        }
        return risultati;
    }

    // Sposta una prenotazione su una nuova proiezione. Entrambe le date (vecchia e nuova)
    // devono essere future; ritorna false se un vincolo è violato.
    public boolean modificaData(String codice, LocalDateTime nuovaDataOra) throws SQLException {
        Prenotazione daModificare = ottieniPrenotazione(codice);
        if (daModificare == null) return false;

        LocalDateTime ora = LocalDateTime.now();
        if (!daModificare.getProiezione().getDataOra().isAfter(ora)) return false;
        if (!nuovaDataOra.isAfter(ora)) return false;
        if (daModificare.getProiezione().getDataOra().equals(nuovaDataOra)) return false;

        int postiLiberi = calcolaPostiLiberi(nuovaDataOra);
        if (daModificare.getNumeroBiglietti() > postiLiberi) return false;

        String sql = "UPDATE prenotazioni SET data_ora = ? WHERE codice_univoco = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(nuovaDataOra));
            ps.setString   (2, codice);
            ps.executeUpdate();
            return true;
        }
    }

    // Cancella una prenotazione solo se la proiezione è ancora futura.
    public boolean cancella(String codice) throws SQLException {
        Prenotazione p = ottieniPrenotazione(codice);
        if (p == null) return false;
        if (!p.getProiezione().getDataOra().isAfter(LocalDateTime.now())) return false;

        String sql = "DELETE FROM prenotazioni WHERE codice_univoco = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codice);
            ps.executeUpdate();
            return true;
        }
    }

    // Cerca prenotazioni per criteri combinabili (usata dal bigliettaio).
    public List<Prenotazione> cerca(CriteriRicercaPrenotazione criteri) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT pr.codice_univoco, pr.numero_posti, pr.data_creazione, " +
                        "u.username, u.nome, u.cognome, u.domicilio, u.data_nascita, u.ruolo, " +
                        "p.data_ora, p.prezzo_biglietto, " +
                        "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                        "(" + CAPIENZA_MASSIMA + " - COALESCE((SELECT SUM(x.numero_posti) FROM prenotazioni x " +
                        "WHERE x.data_ora = p.data_ora), 0)) AS posti_liberi " +
                        "FROM prenotazioni pr " +
                        "JOIN utenti     u ON pr.username_cliente = u.username " +
                        "JOIN proiezioni p ON pr.data_ora         = p.data_ora " +
                        "JOIN film       f ON p.id_film           = f.id_film " +
                        "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (criteri.getCodice() != null && !criteri.getCodice().isBlank()) {
            sql.append("AND pr.codice_univoco = ? ");
            params.add(criteri.getCodice().trim().toUpperCase());
        }
        if (criteri.getNomeCliente() != null && !criteri.getNomeCliente().isBlank()) {
            sql.append("AND (LOWER(u.nome) LIKE LOWER(?) OR LOWER(u.cognome) LIKE LOWER(?)) ");
            params.add("%" + criteri.getNomeCliente().trim() + "%");
            params.add("%" + criteri.getNomeCliente().trim() + "%");
        }
        if (criteri.getCognomeCliente() != null && !criteri.getCognomeCliente().isBlank()) {
            sql.append("AND LOWER(u.cognome) LIKE LOWER(?) ");
            params.add("%" + criteri.getCognomeCliente().trim() + "%");
        }
        if (criteri.getTitoloFilm() != null && !criteri.getTitoloFilm().isBlank()) {
            sql.append("AND LOWER(f.titolo) LIKE LOWER(?) ");
            params.add("%" + criteri.getTitoloFilm().trim() + "%");
        }
        if (criteri.getDataInizio() != null) {
            sql.append("AND p.data_ora >= ? ");
            params.add(Timestamp.valueOf(criteri.getDataInizio().atStartOfDay()));
        }
        if (criteri.getDataFine() != null) {
            sql.append("AND p.data_ora <= ? ");
            params.add(Timestamp.valueOf(criteri.getDataFine().atTime(23, 59, 59)));
        }

        sql.append("ORDER BY p.data_ora");

        List<Prenotazione> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) risultati.add(creaPrenotazione(rs));
            }
        }
        return risultati;
    }

    public List<Prenotazione> trovaDiOggi() throws SQLException {
        String sql = "SELECT pr.codice_univoco, pr.numero_posti, pr.data_creazione, " +
                "u.username, u.nome, u.cognome, u.domicilio, u.data_nascita, u.ruolo, " +
                "p.data_ora, p.prezzo_biglietto, " +
                "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                "(" + CAPIENZA_MASSIMA + " - COALESCE((SELECT SUM(x.numero_posti) FROM prenotazioni x " +
                "WHERE x.data_ora = p.data_ora), 0)) AS posti_liberi " +
                "FROM prenotazioni pr " +
                "JOIN utenti     u ON pr.username_cliente = u.username " +
                "JOIN proiezioni p ON pr.data_ora         = p.data_ora " +
                "JOIN film       f ON p.id_film           = f.id_film " +
                "WHERE p.data_ora::date = CURRENT_DATE " +
                "ORDER BY p.data_ora";

        List<Prenotazione> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) risultati.add(creaPrenotazione(rs));
        }
        return risultati;
    }

    private int calcolaPostiLiberi(LocalDateTime dataOra) throws SQLException {
        String sql = "SELECT COALESCE(SUM(numero_posti), 0) FROM prenotazioni WHERE data_ora = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(dataOra));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return CAPIENZA_MASSIMA - rs.getInt(1);
            }
        }
    }

    private Prenotazione creaPrenotazione(ResultSet rs) throws SQLException {
        Film film = new Film(
                rs.getInt   ("id_film"),
                rs.getString("titolo"),
                rs.getString("genere"),
                rs.getString("regista"),
                rs.getInt   ("anno"),
                rs.getInt   ("durata_minuti"),
                rs.getInt   ("eta_minima")
        );

        Proiezione proiezione = new Proiezione(
                0,
                film,
                rs.getTimestamp("data_ora").toLocalDateTime(),
                rs.getDouble   ("prezzo_biglietto"),
                rs.getInt      ("posti_liberi")
        );

        Date sqlDate = rs.getDate("data_nascita");
        Utente cliente = new Utente(
                rs.getString("nome"),
                rs.getString("cognome"),
                rs.getString("username"),
                null,
                sqlDate != null ? sqlDate.toLocalDate() : null,
                rs.getString("domicilio"),
                Ruolo.valueOf(rs.getString("ruolo"))
        );

        Timestamp tsCreazione = rs.getTimestamp("data_creazione");

        return new Prenotazione(
                rs.getString("codice_univoco"),
                cliente,
                proiezione,
                rs.getInt   ("numero_posti"),
                tsCreazione != null ? tsCreazione.toLocalDateTime() : null
        );
    }
}