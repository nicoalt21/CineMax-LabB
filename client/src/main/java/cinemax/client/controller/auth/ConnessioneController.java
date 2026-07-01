package cinemax.client.controller.auth;

import cinemax.client.gui.navigation.GestoreScene;
import cinemax.client.service.FornitoreServizi;
import cinemax.client.service.StatoConnessione;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Schermata di connessione al server, mostrata all'avvio del client prima dello Start.
 * L'utente indica indirizzo IP e porta del server CineMax.
 * <p>
 * La connessione è reale: si tenta {@code FornitoreServizi.creaReale(ip, porta)} verso il
 * registry RMI; se riesce si avvia il monitoraggio della connessione (StatoConnessione),
 * si inietta il fornitore reale nel GestoreScene e si prosegue allo Start; se fallisce si
 * resta su questa schermata con un messaggio, mantenendo i campi.
 * <p>
 * Il tentativo gira su un thread separato per non bloccare la UI: durante l'attesa il
 * bottone è disabilitato e mostra "Connessione in corso...".
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class ConnessioneController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    private final TextField campoIp = new TextField();
    private final TextField campoPorta = new TextField();
    private final Label labelErrore = new Label();
    private final Button btnConnetti = new Button("Connetti");

    /**
     * Costruisce la schermata di connessione.
     *
     * @param gestoreScene gestore di navigazione, in cui iniettare il fornitore reale
     */
    public ConnessioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    /** @return il nodo radice della schermata, da inserire nella Scene. */
    public VBox getRoot() {
        return radice;
    }

    /**
     * Costruisce la vista: campi indirizzo/porta (con valori di default per i test in
     * locale), bottone Connetti e area errore.
     *
     * @return il contenitore radice della schermata
     */
    private VBox costruisciVista() {
        VBox contenitore = new VBox(20);
        contenitore.setAlignment(Pos.CENTER);
        contenitore.getStyleClass().add("sfondo-principale");
        contenitore.setPadding(new Insets(40));

        Label titolo = new Label("Connessione al server");
        titolo.getStyleClass().add("titolo-principale");

        Label sottotitolo = new Label("Indica indirizzo e porta del server CineMax.");
        sottotitolo.getStyleClass().add("testo-secondario");

        // Valori predefiniti comodi per i test in locale.
        campoIp.setText("localhost");
        campoIp.setPromptText("Indirizzo IP");
        campoIp.getStyleClass().add("campo-testo");
        campoIp.setMaxWidth(280);

        campoPorta.setText("1099"); // porta di default del registro RMI
        campoPorta.setPromptText("Porta");
        campoPorta.getStyleClass().add("campo-testo");
        campoPorta.setMaxWidth(280);

        btnConnetti.setMaxWidth(280);
        btnConnetti.getStyleClass().add("bottone-primario");
        btnConnetti.setOnAction(e -> onConnettiCliccato());

        labelErrore.getStyleClass().add("campo-errore");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);

        contenitore.getChildren().addAll(titolo, sottotitolo, campoIp, campoPorta, btnConnetti, labelErrore);
        return contenitore;
    }

    /**
     * Valida i campi, poi tenta la connessione al registry RMI su un thread separato per
     * non bloccare la UI. In caso di successo avvia il monitoraggio, inietta il fornitore
     * reale e prosegue allo Start; in caso di errore mostra un messaggio.
     */
    private void onConnettiCliccato() {
        pulisciErrore();

        String ip = campoIp.getText();
        String portaTesto = campoPorta.getText();

        if (ip == null || ip.isBlank() || portaTesto == null || portaTesto.isBlank()) {
            mostraErrore("Inserisci indirizzo e porta.");
            return;
        }

        final int porta;
        try {
            porta = Integer.parseInt(portaTesto.trim());
            if (porta < 1 || porta > 65535) {
                mostraErrore("La porta deve essere tra 1 e 65535.");
                return;
            }
        } catch (NumberFormatException ex) {
            mostraErrore("La porta deve essere un numero.");
            return;
        }

        final String host = ip.trim();

        // Tentativo di connessione su thread separato: la rete può richiedere
        // tempo e non deve congelare la UI. Durante l'attesa blocco i campi.
        impostaAttesa(true);

        Thread tentativo = new Thread(() -> {
            try {
                FornitoreServizi reale = FornitoreServizi.creaReale(host, porta);

                // Avvia il monitoraggio: registra il client presso il server (che
                // lo stampa a terminale) e fa partire l'heartbeat. Se anche solo
                // la registrazione fallisce, consideriamo la connessione non valida.
                StatoConnessione.getInstance()
                        .avviaMonitoraggio(reale.getServizioConnessione());

                // Tutto ok: iniettiamo il fornitore reale e proseguiamo, sul thread FX.
                Platform.runLater(() -> {
                    impostaAttesa(false);
                    gestoreScene.setFornitoreServizi(reale);
                    gestoreScene.vaiAStart();
                });

            } catch (Exception ex) {
                // Registry irraggiungibile, servizio non registrato, host errato, ecc.
                Platform.runLater(() -> {
                    impostaAttesa(false);
                    mostraErrore("Server non raggiungibile. Verifica IP e porta e riprova.");
                });
            }
        }, "tentativo-connessione");
        tentativo.setDaemon(true);
        tentativo.start();
    }

    /**
     * Abilita o disabilita i controlli durante il tentativo di connessione, aggiornando
     * il testo del bottone.
     *
     * @param inAttesa true durante il tentativo (controlli bloccati)
     */
    private void impostaAttesa(boolean inAttesa) {
        btnConnetti.setDisable(inAttesa);
        campoIp.setDisable(inAttesa);
        campoPorta.setDisable(inAttesa);
        btnConnetti.setText(inAttesa ? "Connessione in corso..." : "Connetti");
    }

    /**
     * Mostra un messaggio d'errore sotto i campi.
     *
     * @param messaggio testo dell'errore da mostrare
     */
    private void mostraErrore(String messaggio) {
        labelErrore.setText(messaggio);
        labelErrore.setManaged(true);
        labelErrore.setVisible(true);
    }

    /** Nasconde e svuota il messaggio d'errore. */
    private void pulisciErrore() {
        labelErrore.setText("");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
    }
}
