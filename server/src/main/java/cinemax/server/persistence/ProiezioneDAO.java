package cinemax.server.persistence;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per la gestione delle proiezioni nel database.
 * Gestisce le operazioni CRUD sulle proiezioni garantendo che non ci siano
 * sovrapposizioni temporali e che le prenotazioni vengano rispettate.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ProiezioneDAO {

    private static final int CAPIENZA_SALA = 200;

    /**
     * Cerca proiezioni in base ai criteri passati.
     * I campi null di CriteriRicercaProiezione vengono ignorati.
     * Supporta filtri per titolo, genere, intervallo di date e fascia di prezzo.
     *
     * @param criteri criteri di ricerca (titolo, genere, date, costo)
     * @return lista di proiezioni che soddisfano i criteri, mai null
     * @throws SQLException in caso di errore SQL
     */
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

    /**
     * Controlla se un nuovo slot temporale si sovrappone con una proiezione esistente.
     * Due proiezioni si sovrappongono se l'intervallo [inizio, inizio+durata) della
     * nuova proiezione interseca quello di una gia' programmata.
     *
     * @param dataOra     data e ora di inizio della nuova proiezione
     * @param durata      durata in minuti della nuova proiezione
     * @param escludiSlot slot da escludere dal controllo (null se inserimento nuovo,
     *                    valorizzato con la data attuale in caso di modifica)
     * @return true se esiste una sovrapposizione, false altrimenti
     * @throws SQLException in caso di errore SQL
     */
    private boolean siSovrappongono(LocalDateTime dataOra, int durata, LocalDateTime escludiSlot)
           throws SQLException {

       String sql = "SELECT 1 FROM proiezioni p JOIN film f ON p.id_film = f.id_film " +
                       "WHERE (CAST(? AS timestamp) IS NULL OR p.data_ora <> CAST(? AS timestamp)) " +
                       "AND CAST(? AS timestamp) < p.data_ora + make_interval(mins => f.durata_minuti) " +
                       "AND p.data_ora < CAST(? AS timestamp) + make_interval(mins => ?)";

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

    /**
     * Inserisce una nuova proiezione nel palinsesto.
     * Prima verifica che non si sovrapponga temporalmente con altre proiezioni esistenti.
     * Se il film non e' ancora in catalogo, viene inserito automaticamente.
     *
     * @param proiezione proiezione da inserire
     * @return true se inserita con successo, false se c'e' sovrapposizione
     * @throws SQLException in caso di errore SQL
     */
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

    /**
     * Modifica data, film e prezzo di una proiezione esistente.
     * La modifica e' consentita solo se non ci sono prenotazioni per quella proiezione
     * e se il nuovo slot non si sovrappone con altre proiezioni.
     *
     * @param dataOraAttuale data e ora attuale della proiezione da modificare
     * @param idFilm         id del nuovo film
     * @param nuovaDataOra   nuova data e ora della proiezione
     * @param costo          nuovo prezzo del biglietto
     * @return true se modificata con successo, false se ci sono prenotazioni o sovrapposizione
     * @throws SQLException in caso di errore SQL
     */
    public boolean modificaProiezione(LocalDateTime dataOraAttuale, int idFilm,
                                      LocalDateTime nuovaDataOra, double costo) throws SQLException {

        if (hasPrenotazioni(dataOraAttuale)) return false;

        Proiezione p = ottieniProiezione(dataOraAttuale);
        if (p == null) return false;

        int durata = p.getFilm().getDurataMinuti();
        if (siSovrappongono(nuovaDataOra, durata, dataOraAttuale)) return false;

        String sql = "UPDATE proiezioni SET data_ora = ?, id_film = ?, prezzo_biglietto = ? " +
                "WHERE data_ora = ?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(nuovaDataOra));
            ps.setInt      (2, idFilm);
            ps.setDouble   (3, costo);
            ps.setTimestamp(4, Timestamp.valueOf(dataOraAttuale));
            ps.executeUpdate();
            return true;
        }
    }

    /**
     * Elimina una proiezione dal palinsesto.
     * La cancellazione e' consentita solo se non ci sono prenotazioni associate.
     *
     * @param dataOra data e ora della proiezione da eliminare
     * @return true se eliminata con successo, false se ci sono prenotazioni
     * @throws SQLException in caso di errore SQL
     */
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

    /**
     * Carica una singola proiezione per data e ora, con il conteggio aggiornato
     * dei posti liberi calcolato dalle prenotazioni esistenti.
     *
     * @param dataOra data e ora della proiezione
     * @return proiezione trovata oppure null se non esiste
     * @throws SQLException in caso di errore SQL
     */
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

    /**
     * Verifica se una proiezione ha almeno una prenotazione associata.
     * Usato da modificaProiezione ed eliminaProiezione prima di procedere.
     *
     * @param dataOra data e ora della proiezione
     * @return true se esistono prenotazioni, false altrimenti
     * @throws SQLException in caso di errore SQL
     */
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

    /**
     * Inserisce un film nel database se non esiste gia' (per titolo+regista+anno),
     * altrimenti restituisce l'id del film gia' presente nel catalogo.
     *
     * @param film film da inserire o cercare
     * @return id_film assegnato dal database
     * @throws SQLException in caso di errore SQL
     */
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

    /**
     * Calcola gli slot orari liberi (multipli di 5 minuti) in cui un film
     * della durata indicata puo' iniziare nel giorno specificato senza
     * sovrapporsi ad altre proiezioni gia' programmate.
     *
     * @param giorno                       giorno per cui calcolare le finestre libere
     * @param durataMinuti                 durata del film in minuti
     * @param dataOraProiezioneDaEscludere proiezione in modifica da ignorare (null se nuova)
     * @return lista di orari di inizio disponibili
     * @throws SQLException in caso di errore SQL
     */
    public List<java.time.LocalTime> calcolaFinestreLibere(java.time.LocalDate giorno,
                                                           int durataMinuti,
                                                           LocalDateTime dataOraProiezioneDaEscludere)
            throws SQLException {

        List<java.time.LocalTime> risultati = new ArrayList<>();
        java.time.LocalDateTime inizioGiorno = giorno.atStartOfDay();
        java.time.LocalDateTime fineGiorno   = giorno.atTime(23, 59, 59);

        // Carico le proiezioni del giorno (escludo quella in modifica se presente)
        String sql = "SELECT p.data_ora, f.durata_minuti FROM proiezioni p " +
                "JOIN film f ON p.id_film = f.id_film " +
                "WHERE p.data_ora >= ? AND p.data_ora <= ? " +
                (dataOraProiezioneDaEscludere != null ? "AND p.data_ora != ? " : "") +
                "ORDER BY p.data_ora";

        List<long[]> occupati = new ArrayList<>(); // [inizioMinuti, fineMinuti] dalla mezzanotte

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inizioGiorno));
            ps.setTimestamp(2, Timestamp.valueOf(fineGiorno));
            if (dataOraProiezioneDaEscludere != null) {
                ps.setTimestamp(3, Timestamp.valueOf(dataOraProiezioneDaEscludere));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDateTime inizio = rs.getTimestamp("data_ora").toLocalDateTime();
                    int durata = rs.getInt("durata_minuti");
                    long inizioMin = inizio.getHour() * 60L + inizio.getMinute();
                    long fineMin   = inizioMin + durata;
                    // Arrotonda la fine al multiplo di 5 successivo
                    if (fineMin % 5 != 0) fineMin = (fineMin / 5 + 1) * 5;
                    occupati.add(new long[]{inizioMin, fineMin});
                }
            }
        }

        // Prova ogni slot da 0:00 a (1440 - durataMinuti), a multipli di 5
        for (int start = 0; start + durataMinuti <= 1440; start += 5) {
            long fine = start + durataMinuti;
            boolean libero = true;
            for (long[] occ : occupati) {
                // Sovrapposizione: [start, fine) interseca [occ[0], occ[1])
                if (start < occ[1] && fine > occ[0]) {
                    libero = false;
                    break;
                }
            }
            if (libero) {
                risultati.add(java.time.LocalTime.of(start / 60, start % 60));
            }
        }
        return risultati;
    }

    /**
     * Trasforma la riga corrente del ResultSet in un oggetto Proiezione
     * con il film associato e i posti liberi aggiornati.
     *
     * @param rs ResultSet posizionato sulla riga corrente
     * @return oggetto Proiezione popolato con i dati della riga
     * @throws SQLException in caso di errore SQL
     */
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
                0,
                film,
                rs.getTimestamp("data_ora").toLocalDateTime(),
                rs.getDouble   ("prezzo_biglietto"),
                rs.getInt      ("posti_liberi")
        );
    }

}
