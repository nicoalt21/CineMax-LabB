package cinemax.client.gui.model;

/**
 * Impostazioni dell'applicazione, condivise per tutta la sessione.
 * <p>
 * Un'unica istanza è tenuta dal GestoreScene e letta dalle varie schermate, così una
 * modifica fatta nella sezione "Impostazioni" vale ovunque e non si perde navigando.
 * <p>
 * Per ora contiene solo il numero di risultati per pagina; in futuro potrà contenere
 * altre preferenze (tema, formato date, ...).
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class Impostazioni {

    /** Valore di default per i risultati per pagina. */
    public static final int RISULTATI_PER_PAGINA_DEFAULT = 8;

    private int risultatiPerPagina = RISULTATI_PER_PAGINA_DEFAULT;

    /** @return numero di risultati mostrati per pagina nelle liste paginate. */
    public int getRisultatiPerPagina() {
        return risultatiPerPagina;
    }

    /**
     * Imposta i risultati per pagina. Valori non positivi vengono ignorati, mantenendo
     * il valore precedente (la UI dovrebbe comunque validare l'input).
     *
     * @param risultatiPerPagina nuovo numero di risultati per pagina (deve essere &gt; 0)
     */
    public void setRisultatiPerPagina(int risultatiPerPagina) {
        if (risultatiPerPagina > 0) {
            this.risultatiPerPagina = risultatiPerPagina;
        }
    }
}
