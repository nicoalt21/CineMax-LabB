package cinemax.server.persistence;

import cinemax.common.model.Ruolo;
import cinemax.common.model.Utente;

import java.sql.*;
import java.time.LocalDate;

public class UtenteDAO {

    public boolean registraCliente(Utente utente) throws SQLException {

        if (esiste(utente.getUsername())) {
            return false;
        }

        String sql = "INSERT INTO utenti (username, nome, cognome, password_hash, data_nascita, domicilio, ruolo) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'CLIENTE')";

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

    public Utente autenticaUtente(String username, String passwordHash) throws SQLException {

        String sql = "SELECT * FROM utenti WHERE username = ? AND password_hash = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return creaUtente(rs);
                }
                return null;
            }
        }
    }

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
}