package cinemax.client.service.mock;

import cinemax.common.model.Film;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import cinemax.common.model.Utente;
import cinemax.common.util.Cifrario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Contenitore in memoria dei dati usati dai servizi finti.
 * <p>
 * Esiste un'unica istanza condivisa fra i tre servizi finti
 * ({@link ServizioProiezioniFinto}, {@link ServizioPrenotazioniFinto},
 * {@link ServizioAutenticazioneFinto}) cos&igrave; che, ad esempio, una
 * prenotazione creata risulti visibile anche nelle altre viste.
 * <p>
 * Questa classe &egrave; volutamente temporanea: simula il database e la
 * logica del server. Quando il vero server RMI sar&agrave; disponibile,
 * basta iniettare gli stub remoti al posto dei finti e questa classe non
 * viene pi&ugrave; usata.
 */
public class DatiFinti {

    /** Capienza della sala, come da specifiche (cinema monosala da 200 posti). */
    public static final int CAPIENZA_SALA = 200;

    private final List<Film> film = new ArrayList<>();
    private final List<Proiezione> proiezioni = new ArrayList<>();
    private final List<Utente> utenti = new ArrayList<>();
    private final List<Prenotazione> prenotazioni = new ArrayList<>();

    public DatiFinti() {
        precaricaFilm();
        precaricaProiezioni();
        precaricaUtenti();
        // Le prenotazioni vanno caricate per ultime: referenziano utenti e proiezioni.
        precaricaPrenotazioni();
    }

    private void precaricaFilm() {
        film.add(new Film(1, "Inception", "Fantascienza", "Christopher Nolan", 2010, 148, 13));
        film.add(new Film(2, "Il Padrino", "Drammatico", "Francis Ford Coppola", 1972, 175, 14));
        film.add(new Film(3, "Coco", "Animazione", "Lee Unkrich", 2017, 105, 0));

        // Altri 20 film di prova, con generi ed età minime varie (per testare ricerca,
        // ordinamento, paginazione e il semaforo età).
        String[][] extra = {
            {"Interstellar", "Fantascienza", "Christopher Nolan", "2014", "169", "12"},
            {"Il Re Leone", "Animazione", "Roger Allers", "1994", "88", "0"},
            {"Pulp Fiction", "Thriller", "Quentin Tarantino", "1994", "154", "18"},
            {"La La Land", "Musical", "Damien Chazelle", "2016", "128", "7"},
            {"Matrix", "Fantascienza", "Lana Wachowski", "1999", "136", "16"},
            {"Forrest Gump", "Drammatico", "Robert Zemeckis", "1994", "142", "12"},
            {"Up", "Animazione", "Pete Docter", "2009", "96", "0"},
            {"Joker", "Drammatico", "Todd Phillips", "2019", "122", "18"},
            {"Gladiator", "Azione", "Ridley Scott", "2000", "155", "16"},
            {"Frozen", "Animazione", "Chris Buck", "2013", "102", "0"},
            {"Il Cavaliere Oscuro", "Azione", "Christopher Nolan", "2008", "152", "13"},
            {"Parasite", "Thriller", "Bong Joon-ho", "2019", "132", "16"},
            {"Ratatouille", "Animazione", "Brad Bird", "2007", "111", "0"},
            {"Whiplash", "Drammatico", "Damien Chazelle", "2014", "107", "12"},
            {"Avatar", "Fantascienza", "James Cameron", "2009", "162", "12"},
            {"Il Signore degli Anelli", "Fantasy", "Peter Jackson", "2001", "178", "12"},
            {"Shining", "Horror", "Stanley Kubrick", "1980", "146", "18"},
            {"Toy Story", "Animazione", "John Lasseter", "1995", "81", "0"},
            {"Dune", "Fantascienza", "Denis Villeneuve", "2021", "155", "13"},
            {"Grease", "Musical", "Randal Kleiser", "1978", "110", "7"}
        };
        int id = 4;
        for (String[] f : extra) {
            film.add(new Film(id++, f[0], f[1], f[2],
                    Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5])));
        }
    }

    private void precaricaProiezioni() {
        // Proiezioni distribuite a partire da oggi, cos&igrave; sono visibili
        // nelle viste "future" sia del cliente che del proiezionista.
        LocalDateTime oggi = LocalDateTime.now();

        // postiLiberi iniziale = capienza; viene aggiornato quando si prenota.
        proiezioni.add(new Proiezione(1, film.get(0), oggi.plusDays(2).withHour(21).withMinute(0),
                9.50, CAPIENZA_SALA));
        proiezioni.add(new Proiezione(2, film.get(1), oggi.plusDays(5).withHour(20).withMinute(30),
                11.00, CAPIENZA_SALA));
        proiezioni.add(new Proiezione(3, film.get(2), oggi.plusDays(1).withHour(17).withMinute(0),
                7.00, CAPIENZA_SALA));
        // Una proiezione gi&agrave; passata, utile per la sezione "prenotazioni passate".
        proiezioni.add(new Proiezione(4, film.get(0), oggi.minusDays(7).withHour(18).withMinute(0),
                9.50, CAPIENZA_SALA));

        // Una proiezione per ciascuno dei film extra, sparse nei prossimi ~40 giorni
        // con orari, prezzi e posti liberi variabili (per testare ordinamento e pagine).
        int idProiezione = 5;
        double[] prezzi = {7.00, 8.50, 9.00, 10.00, 11.50, 12.00, 6.50, 13.00};
        int[] ore = {15, 17, 18, 20, 21, 22};
        for (int i = 3; i < film.size(); i++) { // dai film extra (indice 3 in poi)
            int giorni = (i - 2) * 2;            // 2, 4, 6, ... giorni nel futuro
            int ora = ore[i % ore.length];
            double prezzo = prezzi[i % prezzi.length];
            int postiOccupati = (i * 7) % 50;    // capienza un po' variabile
            proiezioni.add(new Proiezione(idProiezione++, film.get(i),
                    oggi.plusDays(giorni).withHour(ora).withMinute(0),
                    prezzo, CAPIENZA_SALA - postiOccupati));
        }
    }

    private void precaricaUtenti() {
        // Le password sono cifrate con lo stesso Cifrario usato dal client,
        // cos&igrave; il login finto pu&ograve; confrontare gli hash.
        utenti.add(new Utente("Mario", "Rossi", "cliente", Cifrario.cifraPassword("cliente"),
                LocalDate.of(1995, 4, 12), "Varese", Ruolo.CLIENTE));
        utenti.add(new Utente("Anna", "Bianchi", "proiezionista", Cifrario.cifraPassword("proiezionista"),
                LocalDate.of(1988, 9, 3), "Como", Ruolo.PROIEZIONISTA));
        utenti.add(new Utente("Luca", "Verdi", "bigliettaio", Cifrario.cifraPassword("bigliettaio"),
                LocalDate.of(1990, 1, 27), "Varese", Ruolo.BIGLIETTAIO));
        // Cliente minorenne (14 anni), utile per testare il blocco età sui film VM.
        utenti.add(new Utente("Giulia", "Neri", "minorenne", Cifrario.cifraPassword("minorenne"),
                LocalDate.now().minusYears(14), "Varese", Ruolo.CLIENTE));
    }

    private void precaricaPrenotazioni() {
        Utente cliente = utenti.get(0); // Mario Rossi (username "cliente")

        // Una prenotazione attiva (proiezione futura): sar&agrave; modificabile.
        Proiezione futura = proiezioni.get(0);
        prenotazioni.add(new Prenotazione(UUID.randomUUID().toString(),
                cliente, futura, 2, LocalDateTime.now().minusDays(1)));
        futura.setPostiLiberi(futura.getPostiLiberi() - 2);

        // Una prenotazione passata (proiezione gi&agrave; avvenuta): sezione passate.
        Proiezione passata = proiezioni.get(3);
        prenotazioni.add(new Prenotazione(UUID.randomUUID().toString(),
                cliente, passata, 3, LocalDateTime.now().minusDays(10)));
        passata.setPostiLiberi(passata.getPostiLiberi() - 3);
    }

    public List<Film> getFilm() {
        return film;
    }

    public List<Proiezione> getProiezioni() {
        return proiezioni;
    }

    public List<Utente> getUtenti() {
        return utenti;
    }

    public List<Prenotazione> getPrenotazioni() {
        return prenotazioni;
    }
}
