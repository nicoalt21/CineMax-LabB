package cinemax.server.persistence;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProiezioneDAO {

    private static final int CAPIENZA_SALA = 200;

    public List<Proiezione> cercaProiezione(CriteriRicercaProiezione criteri) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT p.data_ora, p.prezzo_biglietto, " +
                        "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                        "(" + CAPIENZA_SALA + " - COALESCE((SELECT SUM(pr.numero_posti) FROM prenotazioni pr " +
                        "WHERE pr.data_ora = p.data_ora), 0)) AS posti_liberi " +
                        "FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                        "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (criteri.getTitolo() != null && !criteri.getTitolo().isBlank()) {
            sql.append("AND LOWER(f.titolo) LIKE LOWER(?) ");
            params.add("%" + criteri.getTitolo().trim() + "%");
        }
        if (criteri.getGenere() != null && !criteri.getGenere().isBlank()) {
            sql.append("AND f.genere = ? ");
            params.add(criteri.getGenere().trim());
        }
        if (criteri.getDataInizio() != null) {
            sql.append("AND p.data_ora >= ? ");
            params.add(Timestamp.valueOf(criteri.getDataInizio().atStartOfDay()));
        }
        if (criteri.getDataFine() != null) {
            sql.append("AND p.data_ora <= ? ");
            params.add(Timestamp.valueOf(criteri.getDataFine().atTime(23, 59, 59)));
        }
        if (criteri.getCostoMin() != null) {
            sql.append("AND p.prezzo_biglietto >= ? ");
            params.add(criteri.getCostoMin());
        }
        if (criteri.getCostoMax() != null) {
            sql.append("AND p.prezzo_biglietto <= ? ");
            params.add(criteri.getCostoMax());
        }

        sql.append("ORDER BY p.data_ora");

        List<Proiezione> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    risultati.add(creaProiezione(rs));
                }
            }
        }
        return risultati;
    }

   private boolean siSovrappongono(LocalDateTime dataOra, int durata, LocalDateTime escludiSlot)
           throws SQLException {

       String sql =
               "SELECT 1 FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                       "WHERE (? IS NULL OR p.data_ora != ?) " +
                       "AND ? < p.data_ora + (f.durata_minuti || ' minutes')::interval " +
                       "AND p.data_ora < ? + (? || ' minutes')::interval";

       try (Connection conn = DBconnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

           if (escludiSlot != null) {
               ps.setTimestamp(1, Timestamp.valueOf(escludiSlot));
               ps.setTimestamp(2, Timestamp.valueOf(escludiSlot));
           } else {
               ps.setNull(1, Types.TIMESTAMP);
               ps.setNull(2, Types.TIMESTAMP);
           }
           ps.setTimestamp(3, Timestamp.valueOf(dataOra));
           ps.setTimestamp(4, Timestamp.valueOf(dataOra));
           ps.setInt      (5, durata);

           try (ResultSet rs = ps.executeQuery()) {
               return rs.next();
           }
       }
   }

    public boolean aggiungiProiezione(Proiezione proiezione) throws SQLException{
        if (siSovrappongono(proiezione.getDataOra(), proiezione.getFilm().getDurataMinuti(), null)) {
            return false;
        }

        int idFilm = inserisciORecuperaFilm(proiezione.getFilm());

        String sql = "INSERT INTO proiezioni (id_film, data_ora, prezzo_biglietto) VALUES (?, ?, ?)";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt      (1, idFilm);
            ps.setTimestamp(2, Timestamp.valueOf(proiezione.getDataOra()));
            ps.setDouble   (3, proiezione.getCostoBiglietto());
            ps.executeUpdate();
            return true;
        }
    }

    public boolean modificaProiezione(LocalDateTime dataOraAttuale, LocalDateTime nuovaDataOra) throws SQLException {

        if (hasPrenotazioni(dataOraAttuale)) return false;

        Proiezione p = ottieniProiezione(dataOraAttuale);
        if (p == null) return false;

        if (siSovrappongono(nuovaDataOra, p.getFilm().getDurataMinuti(), dataOraAttuale)) return false;

        String sql = "UPDATE proiezioni SET data_ora = ? WHERE data_ora = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(nuovaDataOra));
            ps.setTimestamp(2, Timestamp.valueOf(dataOraAttuale));
            ps.executeUpdate();
            return true;
        }
    }

    public boolean eliminaProiezione(LocalDateTime dataOra) throws SQLException {

        if (hasPrenotazioni(dataOra)) return false;

        String sql = "DELETE FROM proiezioni WHERE data_ora = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(dataOra));
            ps.executeUpdate();
            return true;
        }
    }

    public Proiezione ottieniProiezione(LocalDateTime dataOra) throws SQLException {
        String sql = "SELECT p.data_ora, p.prezzo_biglietto, " +
                        "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                        "(" + CAPIENZA_SALA + " - COALESCE((SELECT SUM(pr.numero_posti) FROM prenotazioni pr " +
                        "WHERE pr.data_ora = p.data_ora), 0)) AS posti_liberi " +
                        "FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                        "WHERE p.data_ora = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(dataOra));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return creaProiezione(rs);
                return null;
            }
        }
    }

    public boolean hasPrenotazioni(LocalDateTime dataOra) throws SQLException {

        String sql = "SELECT 1 FROM prenotazioni WHERE data_ora = ? LIMIT 1";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(dataOra));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Proiezione> getProiezioniStoriche() throws SQLException {

        String sql = "SELECT p.data_ora, p.prezzo_biglietto, " +
                        "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                        "(" + CAPIENZA_SALA + " - COALESCE((SELECT SUM(pr.numero_posti) FROM prenotazioni pr " +
                        "WHERE pr.data_ora = p.data_ora), 0)) AS posti_liberi " +
                        "FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                        "WHERE p.data_ora < NOW() " +
                        "ORDER BY p.data_ora DESC";

        List<Proiezione> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) risultati.add(creaProiezione(rs));
        }
        return risultati;
    }

    public List<Proiezione> getProiezioniFuture() throws SQLException {

        String sql = "SELECT p.data_ora, p.prezzo_biglietto, " +
                        "f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
                        "(" + CAPIENZA_SALA + " - COALESCE((SELECT SUM(pr.numero_posti) FROM prenotazioni pr " +
                        "WHERE pr.data_ora = p.data_ora), 0)) AS posti_liberi " +
                        "FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                        "WHERE p.data_ora > NOW() " +
                        "ORDER BY p.data_ora";

        List<Proiezione> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) risultati.add(creaProiezione(rs));
        }
        return risultati;
    }

    private int inserisciORecuperaFilm(Film film) throws SQLException {

        String selectSql = "SELECT id_film FROM film WHERE titolo = ? AND regista = ? AND anno = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {

            ps.setString(1, film.getTitolo());
            ps.setString(2, film.getRegista());
            ps.setInt   (3, film.getAnno());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_film");
            }
        }

        // Film non trovato: inseriscilo
        String insertSql = "INSERT INTO film (titolo, genere, regista, anno, durata_minuti, eta_minima) " +
                        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_film";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setString(1, film.getTitolo());
            ps.setString(2, film.getGenere());
            ps.setString(3, film.getRegista());
            ps.setInt   (4, film.getAnno());
            ps.setInt   (5, film.getDurataMinuti());
            ps.setInt   (6, film.getEtaMinima());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int id = rs.getInt("id_film");
                film.setIdFilm(id);
                return id;
            }
        }
    }

    private Proiezione creaProiezione(ResultSet rs) throws SQLException {

        Film film = new Film(
                rs.getInt   ("id_film"),
                rs.getString("titolo"),
                rs.getString("genere"),
                rs.getString("regista"),
                rs.getInt   ("anno"),
                rs.getInt   ("durata_minuti"),
                rs.getInt   ("eta_minima")
        );

        return new Proiezione(
                film,
                rs.getTimestamp("data_ora").toLocalDateTime(),
                rs.getDouble   ("prezzo_biglietto"),
                rs.getInt      ("posti_liberi")
        );
    }

}
