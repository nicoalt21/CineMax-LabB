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
 * Implementazione finta di {@link ServizioProiezioni} basata sui dati in
 * memoria di {@link DatiFinti}. Non comunica via rete.
 */
public class ServizioProiezioniFinto implements ServizioProiezioni {

    private static final int PASSO_SLOT_MINUTI = 5;

    private final DatiFinti dati;

    public ServizioProiezioniFinto(DatiFinti dati) {
        this.dati = dati;
    }

    @Override
    public List<Proiezione> cercaProiezioni(CriteriRicercaProiezione criteri) {
        List<Proiezione> risultati = new ArrayList<>();
        for (Proiezione p : dati.getProiezioni()) {
            if (corrisponde(p, criteri)) risultati.add(p);
        }
        return risultati;
    }

    @Override
    public Proiezione visualizzaProiezione(LocalDateTime dataOra) {
        return trovaPerDataOra(dataOra);
    }

    @Override
    public boolean aggiungiProiezione(Proiezione proiezione) {
        if (proiezione == null || proiezione.getFilm() == null || proiezione.getDataOra() == null)
            return false;
        if (!terminaEntroMezzanotte(proiezione)) return false;
        for (Proiezione p : dati.getProiezioni()) {
            if (siSovrappongono(p, proiezione)) return false;
        }
        int nuovoId = prossimoIdProiezione();
        proiezione.setIdProiezione(nuovoId);
        if (proiezione.getPostiLiberi() == 0) {
            proiezione.setPostiLiberi(DatiFinti.CAPIENZA_SALA);
        }
        dati.getProiezioni().add(proiezione);
        return true;
    }

    @Override
    public boolean modificaProiezione(LocalDateTime dataOraAttuale, int idFilm,
                                      LocalDateTime nuovaDataOra, double costo) {
        Proiezione esistente = trovaPerDataOra(dataOraAttuale);
        if (esistente == null || nuovaDataOra == null || costo < 0) return false;

        Film film = trovaFilmPerId(idFilm);
        if (film == null) return false;

        Proiezione candidata = new Proiezione(
                esistente.getIdProiezione(), film, nuovaDataOra, costo, esistente.getPostiLiberi());
        if (!terminaEntroMezzanotte(candidata)) return false;

        for (Proiezione p : dati.getProiezioni()) {
            if (p.getDataOra().equals(dataOraAttuale)) continue;
            if (siSovrappongono(p, candidata)) return false;
        }

        esistente.setFilm(film);
        esistente.setDataOra(nuovaDataOra);
        esistente.setCostoBiglietto(costo);
        return true;
    }

    @Override
    public boolean eliminaProiezione(LocalDateTime dataOra) {
        Proiezione p = trovaPerDataOra(dataOra);
        if (p == null) return false;
        return dati.getProiezioni().remove(p);
    }

    @Override
    public boolean aggiungiFilm(Film film) {
        if (film == null || film.getTitolo() == null || film.getTitolo().isBlank()) return false;
        for (Film f : dati.getFilm()) {
            if (filmIdentici(f, film)) return false;
        }
        film.setIdFilm(prossimoIdFilm());
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
            if (filtro.isEmpty() || f.getTitolo().toLowerCase().contains(filtro))
                risultati.add(f);
        }
        return risultati;
    }

    @Override
    public List<LocalTime> finestreLibere(LocalDate giorno, int durataMinuti,
                                          LocalDateTime dataOraProiezioneDaEscludere) {
        List<LocalTime> slot = new ArrayList<>();
        if (giorno == null || durataMinuti <= 0) return slot;

        List<int[]> occupati = new ArrayList<>();
        for (Proiezione p : dati.getProiezioni()) {
            if (dataOraProiezioneDaEscludere != null
                    && p.getDataOra().equals(dataOraProiezioneDaEscludere)) continue;
            if (p.getDataOra() == null || !p.getDataOra().toLocalDate().equals(giorno)) continue;
            int inizio = minutiDaMezzanotte(p.getDataOra());
            int fine = arrotondaPerEccesso(inizio + p.getFilm().getDurataMinuti(), PASSO_SLOT_MINUTI);
            occupati.add(new int[]{inizio, fine});
        }

        int minutiInGiornata = 24 * 60;
        for (int s = 0; s + durataMinuti <= minutiInGiornata; s += PASSO_SLOT_MINUTI) {
            if (slotLibero(s, s + durataMinuti, occupati)) {
                slot.add(LocalTime.of(s / 60, s % 60));
            }
        }
        return slot;
    }

    private Proiezione trovaPerDataOra(LocalDateTime dataOra) {
        for (Proiezione p : dati.getProiezioni()) {
            if (p.getDataOra().equals(dataOra)) return p;
        }
        return null;
    }

    private Film trovaFilmPerId(int idFilm) {
        for (Film f : dati.getFilm()) {
            if (f.getIdFilm() == idFilm) return f;
        }
        return null;
    }

    private int prossimoIdProiezione() {
        int max = 0;
        for (Proiezione p : dati.getProiezioni()) max = Math.max(max, p.getIdProiezione());
        return max + 1;
    }

    private int prossimoIdFilm() {
        int max = 0;
        for (Film f : dati.getFilm()) max = Math.max(max, f.getIdFilm());
        return max + 1;
    }

    private boolean corrisponde(Proiezione p, CriteriRicercaProiezione c) {
        if (c == null) return true;
        if (c.getTitolo() != null && !c.getTitolo().isBlank()) {
            if (!p.getFilm().getTitolo().toLowerCase().contains(c.getTitolo().toLowerCase()))
                return false;
        }
        if (c.getGenere() != null && !c.getGenere().isBlank()) {
            if (!p.getFilm().getGenere().equalsIgnoreCase(c.getGenere())) return false;
        }
        LocalDate dataProiezione = p.getDataOra().toLocalDate();
        if (c.getDataInizio() != null && dataProiezione.isBefore(c.getDataInizio())) return false;
        if (c.getDataFine()   != null && dataProiezione.isAfter(c.getDataFine()))    return false;
        if (c.getCostoMin() != null && p.getCostoBiglietto() < c.getCostoMin()) return false;
        if (c.getCostoMax() != null && p.getCostoBiglietto() > c.getCostoMax()) return false;
        return true;
    }

    private boolean siSovrappongono(Proiezione a, Proiezione b) {
        if (a.getDataOra() == null || b.getDataOra() == null
                || a.getFilm() == null || b.getFilm() == null) return false;
        return a.getDataOra().isBefore(b.getDataOraFine())
                && b.getDataOra().isBefore(a.getDataOraFine());
    }

    private boolean terminaEntroMezzanotte(Proiezione p) {
        LocalDateTime fine = p.getDataOraFine();
        if (fine == null) return false;
        return !fine.isAfter(p.getDataOra().toLocalDate().plusDays(1).atStartOfDay());
    }

    private int minutiDaMezzanotte(LocalDateTime dt) {
        return dt.getHour() * 60 + dt.getMinute();
    }

    private int arrotondaPerEccesso(int valore, int passo) {
        int resto = valore % passo;
        return resto == 0 ? valore : valore + (passo - resto);
    }

    private boolean slotLibero(int inizio, int fine, List<int[]> occupati) {
        for (int[] occ : occupati) {
            if (inizio < occ[1] && occ[0] < fine) return false;
        }
        return true;
    }

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