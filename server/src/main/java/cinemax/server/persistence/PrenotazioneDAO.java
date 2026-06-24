package cinemax.server.persistence;

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

public class PrenotazioneDAO {

    private static final int CAPIENZA_MASSIMA = 200;

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

    public Prenotazione ottieniPrenotazione(String codice) throws SQLException {

        String sql = "SELECT pr.codice_univoco, pr.numero_posti, " +
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

    public String creaPrenotazione(Utente utente, LocalDateTime dataOraProiezione, int numeroBiglietti) throws SQLException {

        if (numeroBiglietti <= 0) return null;

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
                        if (!rs.next()) return null;

                        if (!dataOraProiezione.isAfter(LocalDateTime.now())) return null;

                        postiOccupati = rs.getInt("posti_occupati");
                        etaMinima     = rs.getInt("eta_minima");
                    }
                }

                int postiLiberi = CAPIENZA_MASSIMA - postiOccupati;
                if (numeroBiglietti > postiLiberi) return null;

                int etaCliente = utente.calcolaEta();
                if (etaCliente != -1 && etaCliente < etaMinima) return null;

                String codice = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                String insertSql = "INSERT INTO prenotazioni (codice_univoco, username_cliente, data_ora, numero_posti) " +
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

    public boolean modificaPrenotazione(String codice, LocalDateTime nuovaDataOra) throws SQLException {

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

    public boolean eliminaPrenotazione(String codice) throws SQLException {

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

    public List<Prenotazione> cercaPrenotazione(String codice, String nomeCliente,
                                                String titoloFilm, LocalDateTime dataInizio,
                                                LocalDateTime dataFine) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT pr.codice_univoco, pr.numero_posti, " +
                        "u.username, u.nome, u.cognome, u.domicilio, u.data_nascita, u.ruolo, " +
                        "p.data_ora, p.prezzo_biglietto, " +
                        "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                        "(" + CAPIENZA_MASSIMA + " - COALESCE((SELECT SUM(x.numero_posti) FROM prenotazioni x " +
                        "WHERE x.data_ora = p.data_ora), 0)) AS posti_liberi " +
                        "FROM prenotazioni pr " +
                        "JOIN utenti     u ON pr.username_cliente = u.username " +
                        "JOIN proiezioni p ON pr.data_ora         = p.data_ora " +
                        "JOIN film       f ON p.id_film           = f.id_film " +
                        "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (codice != null && !codice.isBlank()) {
            sql.append("AND pr.codice_univoco = ? ");
            params.add(codice.trim().toUpperCase());
        }
        if (nomeCliente != null && !nomeCliente.isBlank()) {
            sql.append("AND (LOWER(u.nome) LIKE LOWER(?) OR LOWER(u.cognome) LIKE LOWER(?)) ");
            params.add("%" + nomeCliente.trim() + "%");
            params.add("%" + nomeCliente.trim() + "%");
        }
        if (titoloFilm != null && !titoloFilm.isBlank()) {
            sql.append("AND LOWER(f.titolo) LIKE LOWER(?) ");
            params.add("%" + titoloFilm.trim() + "%");
        }
        if (dataInizio != null) {
            sql.append("AND p.data_ora >= ? ");
            params.add(Timestamp.valueOf(dataInizio));
        }
        if (dataFine != null) {
            sql.append("AND p.data_ora <= ? ");
            params.add(Timestamp.valueOf(dataFine));
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

    public List<Prenotazione> visualizzaPrenotazione(Utente utente) throws SQLException {

        String sql = "SELECT pr.codice_univoco, pr.numero_posti, " +
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

            ps.setString(1, utente.getUsername());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) risultati.add(creaPrenotazione(rs));
            }
        }
        return risultati;
    }

    public List<Prenotazione> visualizzaPrenotazioniOggi() throws SQLException {

        String sql = "SELECT pr.codice_univoco, pr.numero_posti, " +
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

        return new Prenotazione(
                rs.getString("codice_univoco"),
                cliente,
                proiezione,
                rs.getInt   ("numero_posti"),
                null
        );
    }

}