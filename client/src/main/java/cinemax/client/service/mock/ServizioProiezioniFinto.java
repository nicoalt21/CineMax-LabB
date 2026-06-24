package cinemax.client.service.mock;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import cinemax.common.remote.ServizioProiezioni;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione finta di {@link ServizioProiezioni} che lavora sui dati in
 * memoria di {@link DatiFinti}. Serve a sviluppare e testare la UI senza il
 * server reale. Non comunica via rete, quindi non lancia mai
 * {@link java.rmi.RemoteException}: la dichiarazione resta solo per rispettare
 * la firma dell'interfaccia remota.
 */
public class ServizioProiezioniFinto implements ServizioProiezioni {

    /** Granularita' degli slot d'inizio proiezione, in minuti. */
    private static final int PASSO_SLOT_MINUTI = 5;

    private final DatiFinti dati;

    public ServizioProiezioniFinto(DatiFinti dati) {
        this.dati = dati;
    }

    @Override
    public List<Proiezione> cercaProiezioni(CriteriRicercaProiezione criteri) {
        List<Proiezione> risultati = new ArrayList<>();
        for (Proiezione p : dati.getProiezioni()) {
            if (corrisponde(p, criteri)) {
                risultati.add(p);
            }
        }
        return risultati;
    }

    @Override
    public Proiezione visualizzaProiezione(int idProiezione) {
        return trovaPerId(idProiezione);
    }

    @Override
    public boolean aggiungiProiezione(Proiezione proiezione) {
        if (proiezione == null || proiezione.getFilm() == null || proiezione.getDataOra() == null) {
            return false;
        }
        // Vincolo: la proiezione deve terminare entro le 24:00 (niente scavalco mezzanotte).
        if (!terminaEntroMezzanotte(proiezione)) {
            return false;
        }
        // Vincolo: nessuna sovrapposizione temporale con le proiezioni esistenti.
        for (Proiezione p : dati.getProiezioni()) {
            if (siSovrappongono(p, proiezione)) {
                return false;
            }
        }
        // Assegna un id progressivo semplice.
        int nuovoId = prossimoIdProiezione();
        proiezione.setIdProiezione(nuovoId);
        if (proiezione.getPostiLiberi() == 0) {
            proiezione.setPostiLiberi(DatiFinti.CAPIENZA_SALA);
        }
        dati.getProiezioni().add(proiezione);
        return true;
    }

    @Override
    public boolean modificaProiezione(int idProiezione, int idFilm, LocalDateTime nuovaDataOra,
                                      double costo) {
        Proiezione esistente = trovaPerId(idProiezione);
        if (esistente == null || nuovaDataOra == null || costo < 0) {
            return false;
        }
        Film film = trovaFilmPerId(idFilm);
        if (film == null) {
            return false;
        }

        // Costruisco una proiezione "candidata" con i nuovi dati per validare i vincoli,
        // SENZA toccare ancora quella reale.
        Proiezione candidata = new Proiezione(idProiezione, film, nuovaDataOra, costo,
                esistente.getPostiLiberi());

        if (!terminaEntroMezzanotte(candidata)) {
            return false;
        }
        // Sovrapposizione con le ALTRE proiezioni: la proiezione che sto modificando va
        // esclusa dal confronto (altrimenti collide con la propria versione precedente).
        for (Proiezione p : dati.getProiezioni()) {
            if (p.getIdProiezione() == idProiezione) {
                continue;
            }
            if (siSovrappongono(p, candidata)) {
                return false;
            }
        }

        // Vincoli rispettati: muto l'oggetto ESISTENTE in-place, cosi' le prenotazioni
        // gia' agganciate alla proiezione restano valide sul nuovo orario.
        esistente.setFilm(film);
        esistente.setDataOra(nuovaDataOra);
        esistente.setCostoBiglietto(costo);
        return true;
    }

    @Override
    public boolean eliminaProiezione(int idProiezione) {
        Proiezione p = trovaPerId(idProiezione);
        if (p == null) {
            return false;
        }
        return dati.getProiezioni().remove(p);
    }

    @Override
    public boolean aggiungiFilm(Film film) {
        if (film == null || film.getTitolo() == null || film.getTitolo().isBlank()) {
            return false;
        }
        // Vietata la creazione solo se esiste un film IDENTICO su TUTTI i campi
        // (titolo, genere, regista, anno, durata, eta' minima). Titoli uguali ma con
        // altri dati diversi (es. remake) sono ammessi: il film e' identificato da idFilm.
        for (Film f : dati.getFilm()) {
            if (filmIdentici(f, film)) {
                return false;
            }
        }
        int nuovoId = prossimoIdFilm();
        film.setIdFilm(nuovoId);
        dati.getFilm().add(film);
        return true;
    }

    @Override
    public List<Film> elencaFilm() {
        return new ArrayList<>(dati.getFilm());
    }

    @Override
    public List<Film> cercaFilmPerTitolo(String titoloParziale) {
        List<Film> risultati = new ArrayList<>();
        String filtro = titoloParziale == null ? "" : titoloParziale.trim().toLowerCase();
        for (Film f : dati.getFilm()) {
            if (filtro.isEmpty() || f.getTitolo().toLowerCase().contains(filtro)) {
                risultati.add(f);
            }
        }
        return risultati;
    }

    @Override
    public List<LocalTime> finestreLibere(LocalDate giorno, int durataMinuti,
                                          int idProiezioneDaEscludere) {
        List<LocalTime> slot = new ArrayList<>();
        if (giorno == null || durataMinuti <= 0) {
            return slot;
        }

        // Intervalli (in minuti dalla mezzanotte) gia' occupati nel giorno scelto, con la
        // fine arrotondata per ECCESSO al multiplo di 5, cosi' lo slot successivo e' pulito.
        List<int[]> occupati = new ArrayList<>();
        for (Proiezione p : dati.getProiezioni()) {
            if (p.getIdProiezione() == idProiezioneDaEscludere) {
                continue;
            }
            if (p.getDataOra() == null || !p.getDataOra().toLocalDate().equals(giorno)) {
                continue;
            }
            int inizio = minutiDaMezzanotte(p.getDataOra());
            int fine = inizio + p.getFilm().getDurataMinuti();
            fine = arrotondaPerEccesso(fine, PASSO_SLOT_MINUTI);
            occupati.add(new int[]{inizio, fine});
        }

        // Genero gli slot d'inizio candidati ogni 5 minuti, da 00:00 fino all'ultimo
        // istante utile per cui il film termina entro le 24:00 (1440 minuti).
        int minutiInGiornata = 24 * 60;
        for (int s = 0; s + durataMinuti <= minutiInGiornata; s += PASSO_SLOT_MINUTI) {
            int fineSlot = s + durataMinuti;
            if (slotLibero(s, fineSlot, occupati)) {
                slot.add(LocalTime.of(s / 60, s % 60));
            }
        }
        return slot;
    }

    // --- metodi di supporto ---

    private Proiezione trovaPerId(int idProiezione) {
        for (Proiezione p : dati.getProiezioni()) {
            if (p.getIdProiezione() == idProiezione) {
                return p;
            }
        }
        return null;
    }

    private Film trovaFilmPerId(int idFilm) {
        for (Film f : dati.getFilm()) {
            if (f.getIdFilm() == idFilm) {
                return f;
            }
        }
        return null;
    }

    // Id progressivo robusto: massimo id esistente + 1 (evita collisioni dopo le elimina).
    private int prossimoIdProiezione() {
        int max = 0;
        for (Proiezione p : dati.getProiezioni()) {
            max = Math.max(max, p.getIdProiezione());
        }
        return max + 1;
    }

    private int prossimoIdFilm() {
        int max = 0;
        for (Film f : dati.getFilm()) {
            max = Math.max(max, f.getIdFilm());
        }
        return max + 1;
    }

    /**
     * Applica i criteri di ricerca. Un criterio nullo viene ignorato, cos&igrave;
     * &egrave; possibile combinare liberamente i filtri (titolo, genere,
     * intervallo di date, intervallo di costo).
     */
    private boolean corrisponde(Proiezione p, CriteriRicercaProiezione c) {
        if (c == null) {
            return true;
        }
        if (c.getTitolo() != null && !c.getTitolo().isBlank()) {
            String titolo = p.getFilm().getTitolo().toLowerCase();
            if (!titolo.contains(c.getTitolo().toLowerCase())) {
                return false;
            }
        }
        if (c.getGenere() != null && !c.getGenere().isBlank()) {
            if (!p.getFilm().getGenere().equalsIgnoreCase(c.getGenere())) {
                return false;
            }
        }
        LocalDate dataProiezione = p.getDataOra().toLocalDate();
        if (c.getDataInizio() != null && dataProiezione.isBefore(c.getDataInizio())) {
            return false;
        }
        if (c.getDataFine() != null && dataProiezione.isAfter(c.getDataFine())) {
            return false;
        }
        if (c.getCostoMin() != null && p.getCostoBiglietto() < c.getCostoMin()) {
            return false;
        }
        if (c.getCostoMax() != null && p.getCostoBiglietto() > c.getCostoMax()) {
            return false;
        }
        return true;
    }

    /**
     * Due proiezioni si sovrappongono se i rispettivi intervalli di occupazione della sala
     * [inizio, fine) si intersecano. La fine di ciascuna e' inizio + durata del suo film.
     * Proiezioni in giorni diversi non si sovrappongono mai.
     */
    private boolean siSovrappongono(Proiezione a, Proiezione b) {
        if (a.getDataOra() == null || b.getDataOra() == null
                || a.getFilm() == null || b.getFilm() == null) {
            return false;
        }
        LocalDateTime inizioA = a.getDataOra();
        LocalDateTime fineA = a.getDataOraFine();
        LocalDateTime inizioB = b.getDataOra();
        LocalDateTime fineB = b.getDataOraFine();
        // Intersezione di intervalli semiaperti: si toccano agli estremi senza sovrapporsi.
        return inizioA.isBefore(fineB) && inizioB.isBefore(fineA);
    }

    // Vero se la proiezione termina entro le 24:00 dello stesso giorno (niente mezzanotte).
    private boolean terminaEntroMezzanotte(Proiezione p) {
        LocalDateTime inizio = p.getDataOra();
        LocalDateTime fine = p.getDataOraFine();
        if (inizio == null || fine == null) {
            return false;
        }
        // La fine non deve superare la mezzanotte del giorno d'inizio. La fine "esatta" a
        // mezzanotte (00:00 del giorno dopo) e' ammessa come termine entro le 24:00.
        LocalDateTime mezzanotteSuccessiva = inizio.toLocalDate().plusDays(1).atStartOfDay();
        return !fine.isAfter(mezzanotteSuccessiva);
    }

    private int minutiDaMezzanotte(LocalDateTime dt) {
        return dt.getHour() * 60 + dt.getMinute();
    }

    private int arrotondaPerEccesso(int valore, int passo) {
        int resto = valore % passo;
        return resto == 0 ? valore : valore + (passo - resto);
    }

    // Uno slot [inizio, fine) e' libero se non interseca nessun intervallo occupato.
    private boolean slotLibero(int inizio, int fine, List<int[]> occupati) {
        for (int[] occ : occupati) {
            if (inizio < occ[1] && occ[0] < fine) {
                return false;
            }
        }
        return true;
    }

    // Confronto "stesso film" su tutti i campi significativi (idFilm escluso).
    private boolean filmIdentici(Film a, Film b) {
        return a.getTitolo().equalsIgnoreCase(b.getTitolo().trim())
                && uguali(a.getGenere(), b.getGenere())
                && uguali(a.getRegista(), b.getRegista())
                && a.getAnno() == b.getAnno()
                && a.getDurataMinuti() == b.getDurataMinuti()
                && a.getEtaMinima() == b.getEtaMinima();
    }

    private boolean uguali(String x, String y) {
        String sx = x == null ? "" : x.trim();
        String sy = y == null ? "" : y.trim();
        return sx.equalsIgnoreCase(sy);
    }
}
