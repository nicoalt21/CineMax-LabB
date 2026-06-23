package cinemax.client.service.mock;

import cinemax.common.model.Utente;
import cinemax.common.remote.ServizioAutenticazione;

/**
 * Implementazione finta di {@link ServizioAutenticazione} basata sui dati in
 * memoria di {@link DatiFinti}. Non comunica via rete, quindi non lancia mai
 * {@link java.rmi.RemoteException}.
 * <p>
 * Utenti di prova disponibili (username / password):
 * <ul>
 *   <li>cliente / cliente</li>
 *   <li>proiezionista / proiezionista</li>
 *   <li>bigliettaio / bigliettaio</li>
 * </ul>
 */
public class ServizioAutenticazioneFinto implements ServizioAutenticazione {

    private final DatiFinti dati;

    public ServizioAutenticazioneFinto(DatiFinti dati) {
        this.dati = dati;
    }

    /**
     * Verifica le credenziali confrontando l'hash ricevuto con quello
     * memorizzato. Il client cifra la password con
     * {@link cinemax.common.util.Cifrario} prima di chiamare questo metodo,
     * quindi {@code passwordHash} &egrave; gi&agrave; cifrata.
     *
     * @return l'utente se le credenziali sono corrette, altrimenti {@code null}
     */
    @Override
    public Utente login(String username, String passwordHash) {
        for (Utente u : dati.getUtenti()) {
            if (u.getUsername().equals(username)
                    && u.getPasswordCifrata().equals(passwordHash)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Registra un nuovo cliente. Fallisce se lo username &egrave; gi&agrave;
     * in uso. L'utente arriva gi&agrave; con la password cifrata dal client.
     */
    @Override
    public boolean registraCliente(Utente utente) {
        if (utente == null || utente.getUsername() == null) {
            return false;
        }
        for (Utente u : dati.getUtenti()) {
            if (u.getUsername().equals(utente.getUsername())) {
                return false;
            }
        }
        dati.getUtenti().add(utente);
        return true;
    }

    /**
     * Nel finto il logout non ha stato lato server da liberare: &egrave; un
     * no-op che esiste solo per rispettare la firma dell'interfaccia.
     */
    @Override
    public void logout(String username) {
        // niente da fare nel finto
    }
}
