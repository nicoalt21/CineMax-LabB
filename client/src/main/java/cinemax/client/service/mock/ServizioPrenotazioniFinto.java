package cinemax.client.service.mock;

import cinemax.common.model.CriteriRicercaPrenotazione;
import cinemax.common.model.Prenotazione;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Utente;
import cinemax.common.remote.ServizioPrenotazioni;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementazione finta di {@link ServizioPrenotazioni} basata sui dati in
 * memoria di {@link DatiFinti}. Non comunica via rete.
 */
public class ServizioPrenotazioniFinto implements ServizioPrenotazioni {

    private final DatiFinti dati;

    public ServizioPrenotazioniFinto(DatiFinti dati) {
        this.dati = dati;
    }

    @Override
    public Prenotazione creaPrenotazione(LocalDateTime dataOraProiezione,
                                         String usernameCliente,
                                         int numeroBiglietti) {
        Proiezione proiezione = trovaProiezionePerDataOra(dataOraProiezione);
        Utente cliente = trovaUtente(usernameCliente);
        if (proiezione == null || cliente == null) return null;
        if (numeroBiglietti <= 0 || numeroBiglietti > proiezione.getPostiLiberi()) return null;

        String codice = UUID.randomUUID().toString();
        Prenotazione prenotazione = new Prenotazione(
                codice, cliente, proiezione, numeroBiglietti, LocalDateTime.now());
        dati.getPrenotazioni().add(prenotazione);
        proiezione.setPostiLiberi(proiezione.getPostiLiberi() - numeroBiglietti);
        return prenotazione;
    }

    @Override
    public List<Prenotazione> visualizzaPrenotazioniCliente(String usernameCliente) {
        List<Prenotazione> risultati = new ArrayList<>();
        for (Prenotazione p : dati.getPrenotazioni()) {
            if (p.getCliente() != null
                    && p.getCliente().getUsername().equals(usernameCliente)) {
                risultati.add(p);
            }
        }
        return risultati;
    }

    @Override
    public boolean modificaPrenotazione(String codicePrenotazione, LocalDateTime nuovaDataOra) {
        Prenotazione p = trovaPerCodice(codicePrenotazione);
        if (p == null) return false;
        p.getProiezione().setDataOra(nuovaDataOra);
        return true;
    }

    @Override
    public boolean cancellaPrenotazione(String codicePrenotazione) {
        Prenotazione p = trovaPerCodice(codicePrenotazione);
        if (p == null) return false;
        Proiezione proiezione = p.getProiezione();
        proiezione.setPostiLiberi(proiezione.getPostiLiberi() + p.getNumeroBiglietti());
        return dati.getPrenotazioni().remove(p);
    }

    @Override
    public List<Prenotazione> cercaPrenotazioni(CriteriRicercaPrenotazione criteri) {
        List<Prenotazione> risultati = new ArrayList<>();
        for (Prenotazione p : dati.getPrenotazioni()) {
            if (corrisponde(p, criteri)) risultati.add(p);
        }
        return risultati;
    }

    @Override
    public List<Prenotazione> visualizzaPrenotazioniOggi() {
        List<Prenotazione> risultati = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        for (Prenotazione p : dati.getPrenotazioni()) {
            if (p.getProiezione().getDataOra().toLocalDate().equals(oggi)) {
                risultati.add(p);
            }
        }
        return risultati;
    }

    private Prenotazione trovaPerCodice(String codice) {
        for (Prenotazione p : dati.getPrenotazioni()) {
            if (p.getCodice().equals(codice)) return p;
        }
        return null;
    }

    private Proiezione trovaProiezionePerDataOra(LocalDateTime dataOra) {
        for (Proiezione p : dati.getProiezioni()) {
            if (p.getDataOra().equals(dataOra)) return p;
        }
        return null;
    }

    private Utente trovaUtente(String username) {
        for (Utente u : dati.getUtenti()) {
            if (u.getUsername().equals(username)) return u;
        }
        return null;
    }

    private boolean corrisponde(Prenotazione p, CriteriRicercaPrenotazione c) {
        if (c == null) return true;
        if (c.getCodice() != null && !c.getCodice().isBlank()) {
            if (!p.getCodice().equalsIgnoreCase(c.getCodice())) return false;
        }
        Utente cliente = p.getCliente();
        if (c.getNomeCliente() != null && !c.getNomeCliente().isBlank()) {
            if (cliente == null || !cliente.getNome().equalsIgnoreCase(c.getNomeCliente()))
                return false;
        }
        if (c.getCognomeCliente() != null && !c.getCognomeCliente().isBlank()) {
            if (cliente == null || !cliente.getCognome().equalsIgnoreCase(c.getCognomeCliente()))
                return false;
        }
        if (c.getTitoloFilm() != null && !c.getTitoloFilm().isBlank()) {
            String titolo = p.getProiezione().getFilm().getTitolo().toLowerCase();
            if (!titolo.contains(c.getTitoloFilm().toLowerCase())) return false;
        }
        LocalDate dataProiezione = p.getProiezione().getDataOra().toLocalDate();
        if (c.getDataInizio() != null && dataProiezione.isBefore(c.getDataInizio())) return false;
        if (c.getDataFine()   != null && dataProiezione.isAfter(c.getDataFine()))    return false;
        return true;
    }
}
