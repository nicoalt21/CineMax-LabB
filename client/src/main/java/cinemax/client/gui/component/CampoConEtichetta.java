package cinemax.client.gui.component;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Componente riutilizzabile: una riga di form composta da
 * <ul>
 *   <li>etichetta (con eventuale asterisco rosso se il campo è obbligatorio)</li>
 *   <li>un controllo di input (TextField, PasswordField o DatePicker)</li>
 *   <li>una label di errore sotto il campo (nascosta finché non c'è un errore)</li>
 * </ul>
 * Non hardcoda testi, larghezze o stili: tutto arriva dal chiamante o dalle classi CSS
 * condivise (campo-testo, campo-errore, etichetta-campo, ...). In questo modo la stessa
 * classe può essere usata in Login, Registrazione e in qualunque altra form futura.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class CampoConEtichetta extends VBox {

    private static final String CLASSE_ERRORE = "campo-in-errore";

    private final Control controllo;
    private final Label labelErrore = new Label();

    /**
     * Costruisce una riga di form con etichetta, controllo di input e area errore.
     *
     * @param testoEtichetta testo dell'etichetta sopra il campo
     * @param obbligatorio   se true aggiunge l'asterisco rosso di campo obbligatorio
     * @param controllo      controllo di input da incapsulare (TextField, PasswordField, DatePicker)
     * @param larghezza      larghezza preferita del campo e della riga
     */
    public CampoConEtichetta(String testoEtichetta, boolean obbligatorio, Control controllo, double larghezza) {
        super(4);
        this.controllo = controllo;

        setMaxWidth(larghezza);

        getChildren().add(costruisciEtichetta(testoEtichetta, obbligatorio));

        controllo.getStyleClass().add("campo-testo");
        controllo.setMaxWidth(Double.MAX_VALUE);
        controllo.setPrefWidth(larghezza);
        getChildren().add(controllo);

        labelErrore.getStyleClass().add("campo-errore");
        labelErrore.setWrapText(true);
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
        getChildren().add(labelErrore);
    }

    /**
     * Costruisce la riga dell'etichetta, con asterisco opzionale per i campi obbligatori.
     *
     * @param testoEtichetta testo dell'etichetta
     * @param obbligatorio   se true accoda l'asterisco rosso
     * @return la riga (HBox) contenente etichetta ed eventuale asterisco
     */
    private HBox costruisciEtichetta(String testoEtichetta, boolean obbligatorio) {
        HBox riga = new HBox(2);
        Label label = new Label(testoEtichetta);
        label.getStyleClass().add("etichetta-campo");
        riga.getChildren().add(label);
        if (obbligatorio) {
            Label asterisco = new Label("*");
            asterisco.getStyleClass().add("etichetta-obbligatorio");
            riga.getChildren().add(asterisco);
        }
        return riga;
    }

    /**
     * Evidenzia il campo come errato e mostra il messaggio sotto di esso.
     *
     * @param messaggio testo dell'errore (se null o vuoto, l'area messaggio resta nascosta)
     */
    public void mostraErrore(String messaggio) {
        if (!controllo.getStyleClass().contains(CLASSE_ERRORE)) {
            controllo.getStyleClass().add(CLASSE_ERRORE);
        }
        labelErrore.setText(messaggio == null ? "" : messaggio);
        boolean haTesto = messaggio != null && !messaggio.isEmpty();
        labelErrore.setManaged(haTesto);
        labelErrore.setVisible(haTesto);
    }

    /**
     * Evidenzia il campo come errato senza messaggio specifico (usato per errori "globali"
     * mostrati altrove, es. sotto il bottone di conferma).
     */
    public void evidenziaErrore() {
        if (!controllo.getStyleClass().contains(CLASSE_ERRORE)) {
            controllo.getStyleClass().add(CLASSE_ERRORE);
        }
    }

    /** Ripristina lo stato normale del campo e nasconde il messaggio di errore. */
    public void pulisciErrore() {
        controllo.getStyleClass().remove(CLASSE_ERRORE);
        labelErrore.setText("");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
    }

    /** @return il controllo di input incapsulato (TextField, PasswordField o DatePicker). */
    public Control getControllo() {
        return controllo;
    }

    /** @return questo stesso componente come Node, per l'inserimento nei layout. */
    public Node getNode() {
        return this;
    }

    /**
     * @return il testo corrente del controllo se è un campo di testo o password,
     *         stringa vuota per gli altri tipi di controllo.
     */
    public String getTesto() {
        if (controllo instanceof TextField) {
            return ((TextField) controllo).getText();
        }
        if (controllo instanceof PasswordField) {
            return ((PasswordField) controllo).getText();
        }
        return "";
    }
}
