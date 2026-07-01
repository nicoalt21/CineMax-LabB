package cinemax.client.controller.shared;

import cinemax.client.gui.model.Impostazioni;
import cinemax.client.gui.navigation.GestoreScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Schermata "Impostazioni" comune a tutti i ruoli, costruita interamente in codice Java.
 * <p>
 * Per ora espone una sola opzione: il numero di risultati per pagina, letto e salvato
 * nell'oggetto Impostazioni condiviso tenuto dal GestoreScene (così la modifica vale per
 * tutta la sessione). La struttura è pensata per accogliere altre opzioni in futuro.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ImpostazioniController extends DashboardBaseController {

    private final GestoreScene gestoreScene;

    private final VBox radice = new VBox(15);
    private final TextField campoRisultatiPerPagina = new TextField();
    private final Label labelMessaggio = new Label();

    /**
     * Costruisce il controller per le impostazioni utente.
     *
     * @param gestoreScene Il gestore delle scene per l'accesso ai servizi e ai modelli.
     */
    public ImpostazioniController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
    }

    /**
     * Restituisce il nodo radice dell'interfaccia grafica.
     *
     * @return Il Parent radice.
     */
    @Override
    public Parent getRoot() {
        return radice;
    }

    /**
     * Inizializza l'interfaccia visiva del pannello delle impostazioni.
     */
    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titolo = new Label("Impostazioni");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        VBox modulo = new VBox(12);
        modulo.setMaxWidth(420);

        Label etichetta = new Label("Numero di risultati per pagina");
        etichetta.getStyleClass().add("etichetta-campo");
        campoRisultatiPerPagina.getStyleClass().add("campo-testo");
        campoRisultatiPerPagina.setMaxWidth(120);

        // Etichetta e campo sulla stessa riga (in linea).
        HBox rigaRisultati = new HBox(12, etichetta, campoRisultatiPerPagina);
        rigaRisultati.setAlignment(Pos.CENTER_LEFT);

        Button btnSalva = new Button("Salva");
        btnSalva.getStyleClass().add("bottone-primario");
        btnSalva.setOnAction(e -> salva());

        labelMessaggio.getStyleClass().add("campo-errore");
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);

        modulo.getChildren().addAll(rigaRisultati, btnSalva, labelMessaggio);
        radice.getChildren().addAll(titolo, modulo);

        aggiornaDati();
    }

    /**
     * Legge le impostazioni correnti per aggiornare i campi dell'interfaccia.
     */
    @Override
    public void aggiornaDati() {
        Impostazioni imp = gestoreScene.getImpostazioni();
        campoRisultatiPerPagina.setText(String.valueOf(imp.getRisultatiPerPagina()));
        pulisciMessaggio();
    }

    /**
     * Salva le modifiche inserite dall'utente se rispettano i criteri di validazione.
     */
    private void salva() {
        pulisciMessaggio();
        try {
            int valore = Integer.parseInt(campoRisultatiPerPagina.getText().trim());
            if (valore < 1 || valore > 100) {
                mostraErrore("Inserisci un numero tra 1 e 100.");
                return;
            }
            gestoreScene.getImpostazioni().setRisultatiPerPagina(valore);
            mostraSuccesso("Impostazioni salvate.");
        } catch (NumberFormatException ex) {
            mostraErrore("Inserisci un numero valido.");
        }
    }

    /**
     * Visualizza un messaggio di errore nell'interfaccia.
     *
     * @param messaggio Il testo dell'errore.
     */
    private void mostraErrore(String messaggio) {
        labelMessaggio.getStyleClass().setAll("campo-errore");
        labelMessaggio.setText(messaggio);
        labelMessaggio.setManaged(true);
        labelMessaggio.setVisible(true);
    }

    /**
     * Visualizza un messaggio di successo nell'interfaccia.
     *
     * @param messaggio Il testo del successo.
     */
    private void mostraSuccesso(String messaggio) {
        labelMessaggio.getStyleClass().setAll("testo-secondario");
        labelMessaggio.setText(messaggio);
        labelMessaggio.setManaged(true);
        labelMessaggio.setVisible(true);
    }

    /**
     * Rimuove messaggi di avviso attivi dallo schermo.
     */
    private void pulisciMessaggio() {
        labelMessaggio.setText("");
        labelMessaggio.setManaged(false);
        labelMessaggio.setVisible(false);
    }
}