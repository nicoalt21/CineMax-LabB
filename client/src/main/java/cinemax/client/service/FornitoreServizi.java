/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.service;

import cinemax.common.remote.ServizioAutenticazione;
import cinemax.common.remote.ServizioConnessione;
import cinemax.common.remote.ServizioPrenotazioni;
import cinemax.common.remote.ServizioProiezioni;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Raccoglie i tre servizi remoti dell'applicazione in un unico oggetto, cos&igrave;
 * i controller ne ricevono uno solo invece di tre parametri separati.
 * <p>
 * &Egrave; questo il punto in cui si collega l'applicazione agli stub RMI reali
 * esposti dal server. I controller dipendono solo dalle interfacce in
 * {@code cinemax.common.remote}, quindi non conoscono i dettagli del trasporto.
 */
public class FornitoreServizi {

    private final ServizioAutenticazione servizioAutenticazione;
    private final ServizioProiezioni servizioProiezioni;
    private final ServizioPrenotazioni servizioPrenotazioni;

    // Servizio di monitoraggio connessione. È null per il fornitore finto
    // (in memoria non c'è alcun server da sorvegliare); valorizzato solo dal
    // fornitore reale.
    private final ServizioConnessione servizioConnessione;

    public FornitoreServizi(ServizioAutenticazione servizioAutenticazione,
                            ServizioProiezioni servizioProiezioni,
                            ServizioPrenotazioni servizioPrenotazioni) {
        this(servizioAutenticazione, servizioProiezioni, servizioPrenotazioni, null);
    }

    public FornitoreServizi(ServizioAutenticazione servizioAutenticazione,
                            ServizioProiezioni servizioProiezioni,
                            ServizioPrenotazioni servizioPrenotazioni,
                            ServizioConnessione servizioConnessione) {
        this.servizioAutenticazione = servizioAutenticazione;
        this.servizioProiezioni = servizioProiezioni;
        this.servizioPrenotazioni = servizioPrenotazioni;
        this.servizioConnessione = servizioConnessione;
    }

    /**
     * Crea un fornitore basato sui servizi RMI reali esposti dal server.
     * Si collega al registry sull'host e porta indicati e recupera gli stub
     * dei tre servizi. I nomi usati nel lookup devono combaciare con quelli
     * registrati lato server in {@code ServerCM} (rebind sul registry).
     *
     * @param host  host del registry RMI (es. "localhost")
     * @param porta porta del registry RMI (es. 1099)
     * @return fornitore collegato ai servizi remoti
     * @throws RemoteException   se il registry non e' raggiungibile
     * @throws NotBoundException se uno dei servizi non risulta registrato
     */
    public static FornitoreServizi creaReale(String host, int porta)
            throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, porta);
        return new FornitoreServizi(
                (ServizioAutenticazione) registry.lookup("ServizioAutenticazione"),
                (ServizioProiezioni)     registry.lookup("ServizioProiezioni"),
                (ServizioPrenotazioni)   registry.lookup("ServizioPrenotazioni"),
                (ServizioConnessione)    registry.lookup("ServizioConnessione"));
    }

    public ServizioAutenticazione getServizioAutenticazione() {
        return servizioAutenticazione;
    }

    public ServizioProiezioni getServizioProiezioni() {
        return servizioProiezioni;
    }

    public ServizioPrenotazioni getServizioPrenotazioni() {
        return servizioPrenotazioni;
    }

    /**
     * Servizio di monitoraggio connessione. {@code null} per il fornitore finto:
     * chi lo usa deve gestire questo caso (in modalità finta non c'è connessione
     * di rete da sorvegliare).
     */
    public ServizioConnessione getServizioConnessione() {
        return servizioConnessione;
    }
}
