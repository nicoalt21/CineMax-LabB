package cinemax.server.persistence;

import cinemax.common.model.Film;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per la gestione dei film nel database.
 */
public class FilmDAO {

    /**
     * Aggiunge un film al catalogo.
     * Fallisce se esiste già un film identico su tutti i campi.
     *
     * @param film film da aggiungere
     * @return true se aggiunto, false se duplicato esatto
     * @throws SQLException in caso di errore DB
     */
    public boolean aggiungi(Film film) throws SQLException {
        String checkSql = "SELECT 1 FROM film WHERE titolo = ? AND genere = ? AND regista = ? " +
                "AND anno = ? AND durata_minuti = ? AND eta_minima = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, film.getTitolo());
            ps.setString(2, film.getGenere());
            ps.setString(3, film.getRegista());
            ps.setInt   (4, film.getAnno());
            ps.setInt   (5, film.getDurataMinuti());
            ps.setInt   (6, film.getEtaMinima());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return false;
            }
        }

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
                film.setIdFilm(rs.getInt("id_film"));
            }
        }
        return true;
    }

    /**
     * Restituisce tutti i film del catalogo.
     *
     * @return lista di tutti i film
     * @throws SQLException in caso di errore DB
     */
    public List<Film> elencaTutti() throws SQLException {
        String sql = "SELECT * FROM film ORDER BY titolo";
        List<Film> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) risultati.add(creaFilm(rs));
        }
        return risultati;
    }

    /**
     * Cerca film per titolo parziale (case-insensitive).
     *
     * @param titoloParziale stringa da cercare nel titolo, null o vuota = tutti
     * @return lista di film il cui titolo contiene la stringa
     * @throws SQLException in caso di errore DB
     */
    public List<Film> cercaPerTitolo(String titoloParziale) throws SQLException {
        if (titoloParziale == null || titoloParziale.isBlank()) {
            return elencaTutti();
        }

        String sql = "SELECT * FROM film WHERE LOWER(titolo) LIKE LOWER(?) ORDER BY titolo";
        List<Film> risultati = new ArrayList<>();

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + titoloParziale.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) risultati.add(creaFilm(rs));
            }
        }
        return risultati;
    }

    private Film creaFilm(ResultSet rs) throws SQLException {
        return new Film(
                rs.getInt   ("id_film"),
                rs.getString("titolo"),
                rs.getString("genere"),
                rs.getString("regista"),
                rs.getInt   ("anno"),
                rs.getInt   ("durata_minuti"),
                rs.getInt   ("eta_minima")
        );
    }
}