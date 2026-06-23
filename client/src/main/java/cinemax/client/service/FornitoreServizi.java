/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.service;

import cinemax.client.service.mock.DatiFinti;
import cinemax.client.service.mock.ServizioAutenticazioneFinto;
import cinemax.client.service.mock.ServizioPrenotazioniFinto;
import cinemax.client.service.mock.ServizioProiezioniFinto;
import cinemax.common.remote.ServizioAutenticazione;
import cinemax.common.remote.ServizioPrenotazioni;
import cinemax.common.remote.ServizioProiezioni;

/**
 * Raccoglie i tre servizi remoti dell'applicazione in un unico oggetto, cos&igrave;
 * i controller ne ricevono uno solo invece di tre parametri separati.
 * <p>
 * &Egrave; questo il punto in cui si sceglie l'implementazione: oggi i servizi
 * finti in memoria, domani gli stub RMI reali. I controller dipendono solo
 * dalle interfacce in {@code cinemax.common.remote}, quindi non si accorgono
 * della differenza.
 */
public class FornitoreServizi {

    private final ServizioAutenticazione servizioAutenticazione;
    private final ServizioProiezioni servizioProiezioni;
    private final ServizioPrenotazioni servizioPrenotazioni;

    public FornitoreServizi(ServizioAutenticazione servizioAutenticazione,
                            ServizioProiezioni servizioProiezioni,
                            ServizioPrenotazioni servizioPrenotazioni) {
        this.servizioAutenticazione = servizioAutenticazione;
        this.servizioProiezioni = servizioProiezioni;
        this.servizioPrenotazioni = servizioPrenotazioni;
    }

    /**
     * Crea un fornitore basato sui servizi finti in memoria, condividendo fra
     * essi la stessa istanza di {@link DatiFinti}. Usato per sviluppare e
     * testare la UI senza il server.
     */
    public static FornitoreServizi creaFinto() {
        DatiFinti dati = new DatiFinti();
        return new FornitoreServizi(
                new ServizioAutenticazioneFinto(dati),
                new ServizioProiezioniFinto(dati),
                new ServizioPrenotazioniFinto(dati));
    }

    /*
     * Quando il server RMI sar&agrave; pronto, qui andr&agrave; un metodo come:
     *
     *   public static FornitoreServizi creaReale(String host, int porta)
     *           throws RemoteException, NotBoundException {
     *       Registry registry = LocateRegistry.getRegistry(host, porta);
     *       return new FornitoreServizi(
     *               (ServizioAutenticazione) registry.lookup("ServizioAutenticazione"),
     *               (ServizioProiezioni)     registry.lookup("ServizioProiezioni"),
     *               (ServizioPrenotazioni)   registry.lookup("ServizioPrenotazioni"));
     *   }
     *
     * Il resto dell'applicazione non cambia.
     */

    public ServizioAutenticazione getServizioAutenticazione() {
        return servizioAutenticazione;
    }

    public ServizioProiezioni getServizioProiezioni() {
        return servizioProiezioni;
    }

    public ServizioPrenotazioni getServizioPrenotazioni() {
        return servizioPrenotazioni;
    }
}
