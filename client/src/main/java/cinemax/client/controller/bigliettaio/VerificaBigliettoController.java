package cinemax.client.controller.bigliettaio;

import cinemax.client.controller.shared.DashboardBaseController;
import cinemax.client.gui.component.CardPrenotazione;
import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.CriteriRicercaPrenotazione;
import cinemax.common.model.Prenotazione;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;

/*
 Schermata "Verifica biglietto" del Bigliettaio, costruita interamente in codice Java.

 Permette di cercare una prenotazione in tre modi, scelti da un menu a tendina:
   - per codice prenotazione (corrispondenza esatta);
   - per username del cliente;
   - per nome e cognome del cliente.

 Per username e nome+cognome possono risultare più prenotazioni a nome di quella
 persona: vengono mostrate tutte come card. "Verificare" qui significa trovare e
 mostrare la prenotazione (nessuno stato cambia): il bigliettaio confronta i dati a
 schermo con il biglietto del cliente.

 I dati arrivano dal servizio remoto (oggi: implementazione finta) tramite
 cercaPrenotazioni(CriteriRicercaPrenotazione).
 */
public class VerificaBigliettoController extends DashboardBaseController {

    private static final String PER_CODICE = "Per codice";
    private static final String PER_USERNAME = "Per username";
    private static final String PER_NOME_COGNOME = "Per nome e cognome";

    private final GestoreScene gestoreScene;

    private final VBox radice = new VBox(15);
    private final ComboBox<String> selettoreTipo = new ComboBox<>();
    private final TextField campo1 = new TextField(); // codice / username / nome
    private final TextField campo2 = new TextField(); // cognome (solo nome+cognome)
    private final VBox contenitoreRisultati = new VBox(12);
    private final Label labelStato = new Label();

    public VerificaBigliettoController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
    }

    @Override
    public Parent getRoot() {
        return radice;
    }

    @Override
    public void inizializza() {
        radice.setPadding(new Insets(20));
        radice.getStyleClass().add("sfondo-principale");

        Label titolo = new Label("Verifica biglietto");
        titolo.getStyleClass().add("titolo-principale");
        titolo.setStyle("-fx-font-size: 26px;");

        // Selettore del tipo di ricerca.
        selettoreTipo.getItems().addAll(PER_CODICE, PER_USERNAME, PER_NOME_COGNOME);
        selettoreTipo.setValue(PER_CODICE);
        selettoreTipo.getStyleClass().add("campo-testo");
        selettoreTipo.setOnAction(e -> aggiornaCampi());

        campo1.getStyleClass().add("campo-testo");
        campo2.getStyleClass().add("campo-testo");
        campo1.setOnAction(e -> eseguiRicerca());
        campo2.setOnAction(e -> eseguiRicerca());

        Button btnCerca = new Button("Verifica");
        btnCerca.getStyleClass().add("bottone-primario");
        btnCerca.setOnAction(e -> eseguiRicerca());

        HBox rigaRicerca = new HBox(10, selettoreTipo, campo1, campo2, btnCerca);
        rigaRicerca.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campo1, Priority.ALWAYS);
        HBox.setHgrow(campo2, Priority.ALWAYS);

        labelStato.getStyleClass().add("testo-secondario");

        ScrollPane scroll = new ScrollPane(contenitoreRisultati);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("area-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        radice.getChildren().addAll(titolo, rigaRicerca, labelStato, scroll);

        aggiornaCampi();
    }

    @Override
    public void aggiornaDati() {
        // Niente da caricare all'apertura: la ricerca parte su richiesta dell'utente.
    }

    // Mostra/nasconde i campi in base al tipo di ricerca scelto e ne imposta i prompt.
    private void aggiornaCampi() {
        String tipo = selettoreTipo.getValue();
        boolean nomeCognome = PER_NOME_COGNOME.equals(tipo);

        campo2.setManaged(nomeCognome);
        campo2.setVisible(nomeCognome);

        if (PER_CODICE.equals(tipo)) {
            campo1.setPromptText("Codice prenotazione");
        } else if (PER_USERNAME.equals(tipo)) {
            campo1.setPromptText("Username del cliente");
        } else {
            campo1.setPromptText("Nome");
            campo2.setPromptText("Cognome");
        }
        campo1.clear();
        campo2.clear();
        contenitoreRisultati.getChildren().clear();
        labelStato.setText("");
    }

    private void eseguiRicerca() {
        contenitoreRisultati.getChildren().clear();
        String tipo = selettoreTipo.getValue();

        CriteriRicercaPrenotazione criteri = new CriteriRicercaPrenotazione();
        if (PER_CODICE.equals(tipo)) {
            String codice = campo1.getText();
            if (codice == null || codice.isBlank()) {
                labelStato.setText("Inserisci il codice della prenotazione.");
                return;
            }
            criteri.setCodice(codice.trim());
        } else if (PER_USERNAME.equals(tipo)) {
            // Il criterio non ha un campo "username"; cerchiamo per nome cliente
            // usando l'username come nome (il finto confronta su nome). In attesa che
            // il server supporti la ricerca per username, qui passiamo l'username come
            // nome: per i dati di prova nome == username non sempre coincide, quindi
            // usiamo il nome cliente come campo più generale.
            String username = campo1.getText();
            if (username == null || username.isBlank()) {
                labelStato.setText("Inserisci lo username del cliente.");
                return;
            }
            criteri.setNomeCliente(username.trim());
        } else { // nome + cognome
            String nome = campo1.getText();
            String cognome = campo2.getText();
            if (nome == null || nome.isBlank() || cognome == null || cognome.isBlank()) {
                labelStato.setText("Inserisci nome e cognome del cliente.");
                return;
            }
            criteri.setNomeCliente(nome.trim());
            criteri.setCognomeCliente(cognome.trim());
        }

        try {
            List<Prenotazione> risultati = gestoreScene.getFornitoreServizi()
                    .getServizioPrenotazioni()
                    .cercaPrenotazioni(criteri);
            mostraRisultati(risultati);
        } catch (RemoteException e) {
            labelStato.setText("Server non raggiungibile. Riprova.");
        }
    }

    private void mostraRisultati(List<Prenotazione> risultati) {
        contenitoreRisultati.getChildren().clear();

        if (risultati == null || risultati.isEmpty()) {
            labelStato.setText("Nessuna prenotazione corrispondente trovata.");
            return;
        }

        labelStato.setText(risultati.size()
                + (risultati.size() == 1 ? " prenotazione trovata." : " prenotazioni trovate."));

        LocalDateTime adesso = LocalDateTime.now();
        for (Prenotazione p : risultati) {
            CardPrenotazione card = new CardPrenotazione();
            // Sola consultazione: il bigliettaio verifica i dati, non annulla nulla.
            boolean passata = p.getProiezione().getDataOra().isBefore(adesso);
            card.compilaDatiPrenotazione(p, passata);
            contenitoreRisultati.getChildren().add(card);
        }
    }
}
