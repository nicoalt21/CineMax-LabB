package cinemax.client.service.mock;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Proiezione;
import cinemax.common.remote.ServizioProiezioni;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        // Vincolo da specifiche: non deve sovrapporsi a una proiezione esistente.
        for (Proiezione p : dati.getProiezioni()) {
            if (siSovrappongono(p, proiezione)) {
                return false;
            }
        }
        // Assegna un id progressivo semplice.
        int nuovoId = dati.getProiezioni().size() + 1;
        proiezione.setIdProiezione(nuovoId);
        if (proiezione.getPostiLiberi() == 0) {
            proiezione.setPostiLiberi(DatiFinti.CAPIENZA_SALA);
        }
        dati.getProiezioni().add(proiezione);
        return true;
    }

    @Override
    public boolean modificaProiezione(int idProiezione, LocalDateTime nuovaDataOra) {
        Proiezione p = trovaPerId(idProiezione);
        if (p == null) {
            return false;
        }
        p.setDataOra(nuovaDataOra);
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

    // --- metodi di supporto ---

    private Proiezione trovaPerId(int idProiezione) {
        for (Proiezione p : dati.getProiezioni()) {
            if (p.getIdProiezione() == idProiezione) {
                return p;
            }
        }
        return null;
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
     * Due proiezioni si sovrappongono se iniziano nello stesso momento.
     * Semplificazione volutamente minima per il finto: la verifica reale
     * (basata sulla durata del film) spetta al server.
     */
    private boolean siSovrappongono(Proiezione esistente, Proiezione nuova) {
        return esistente.getDataOra().equals(nuova.getDataOra());
    }
}
