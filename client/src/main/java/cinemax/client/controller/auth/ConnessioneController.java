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

/*
 Schermata di connessione al server, mostrata all'avvio del client prima dello Start.
 L'utente indica indirizzo IP e porta del server CineMax.

 A differenza del placeholder precedente, qui la connessione è REALE:
   - si tenta FornitoreServizi.creaReale(ip, porta) verso il registry RMI;
   - se riesce, si avvia il monitoraggio della connessione (StatoConnessione),
     che registra il client presso il server (il server lo stampa a terminale)
     e fa partire l'heartbeat periodico;
   - il fornitore reale viene iniettato nel GestoreScene e si prosegue allo Start;
   - se fallisce, si resta su questa schermata con un messaggio, mantenendo i campi.

 Il tentativo gira su un thread separato per non bloccare la UI: durante l'attesa
 il bottone è disabilitato e mostra "Connessione in corso...".

 Costruita interamente in codice Java (niente FXML), in linea con il resto della UI.
 */
public class ConnessioneController {

    private final GestoreScene gestoreScene;
    private final VBox radice;

    private final TextField campoIp = new TextField();
    private final TextField campoPorta = new TextField();
    private final Label labelErrore = new Label();
    private final Button btnConnetti = new Button("Connetti");

    public ConnessioneController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        this.radice = costruisciVista();
    }

    public VBox getRoot() {
        return radice;
    }

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

    // Abilita/disabilita i controlli durante il tentativo di connessione.
    private void impostaAttesa(boolean inAttesa) {
        btnConnetti.setDisable(inAttesa);
        campoIp.setDisable(inAttesa);
        campoPorta.setDisable(inAttesa);
        btnConnetti.setText(inAttesa ? "Connessione in corso..." : "Connetti");
    }

    private void mostraErrore(String messaggio) {
        labelErrore.setText(messaggio);
        labelErrore.setManaged(true);
        labelErrore.setVisible(true);
    }

    private void pulisciErrore() {
        labelErrore.setText("");
        labelErrore.setManaged(false);
        labelErrore.setVisible(false);
    }
}
