/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.gui.util;

import java.time.LocalDate;
import java.time.Period;

/**
 * Utility per il limite di età di un film.
 * <p>
 * Fornisce due cose, riusabili da tutte le viste (card, dettagli, prenotazione):
 * <ul>
 *   <li>la fascia di colore associata all'età minima (semaforo);</li>
 *   <li>il controllo se un utente, data la sua data di nascita, è abbastanza grande.</li>
 * </ul>
 * Le fasce di colore seguono le soglie concordate:
 * <pre>
 *   &lt; 7   verde
 *   7..11  giallo
 *   12..17 arancione
 *   &ge; 18  rosso
 * </pre>
 */
public final class FasciaEta {

    /** Le quattro fasce di colore del semaforo età. */
    public enum Fascia {
        VERDE("fascia-verde"),
        GIALLO("fascia-giallo"),
        ARANCIONE("fascia-arancione"),
        ROSSO("fascia-rosso");

        private final String classeCss;

        Fascia(String classeCss) {
            this.classeCss = classeCss;
        }

        /** Nome della classe CSS che colora il pallino per questa fascia. */
        public String getClasseCss() {
            return classeCss;
        }
    }

    private FasciaEta() {}

    /** Restituisce la fascia di colore corrispondente all'età minima del film. */
    public static Fascia fasciaPerEta(int etaMinima) {
        if (etaMinima >= 18) {
            return Fascia.ROSSO;
        }
        if (etaMinima >= 12) {
            return Fascia.ARANCIONE;
        }
        if (etaMinima >= 7) {
            return Fascia.GIALLO;
        }
        return Fascia.VERDE;
    }

    /**
     * Verifica se un utente è abbastanza grande per un film, data la sua data di nascita.
     * <p>
     * La data di nascita è facoltativa: se è {@code null} (es. utente Guest o utente
     * che non l'ha indicata) l'età non è verificabile e il metodo restituisce
     * {@code true}, lasciando la verifica forte al server/bigliettaio. Il blocco lato
     * client scatta solo quando l'età è nota ed effettivamente insufficiente.
     *
     * @param dataNascita data di nascita dell'utente (può essere null)
     * @param etaMinima   età minima richiesta dal film
     * @return true se la prenotazione è consentita lato client
     */
    public static boolean puoPrenotare(LocalDate dataNascita, int etaMinima) {
        if (dataNascita == null) {
            return true; // età non verificabile: non blocchiamo qui
        }
        int eta = Period.between(dataNascita, LocalDate.now()).getYears();
        return eta >= etaMinima;
    }
}
