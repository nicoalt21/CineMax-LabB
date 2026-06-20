package cinemax.client.controller.shared;

import cinemax.client.gui.navigation.GestoreScene;
import cinemax.common.model.Utente;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/*
 Guscio comune a Client e Guest, costruito interamente in codice Java.
 Header in alto (con il bottone Login/Logout), menu laterale a sinistra, area centrale dove vengono caricate le dashboard.

 La vista del Guest è identica a quella del Client: l'unica differenza è che le parti riservate vengono attenuate e rese non cliccabili.
 Il bottone in alto a destra diventa "Log In" (primario) invece di "Log Out" (bianco).
 */
public class BaseLayoutController {

    private final BorderPane contenitorePrincipale = new BorderPane();
    private final VBox menuLaterale = new VBox(10);
    private final Label labelBenvenuto = new Label();

    // Bottone Login/Logout (cambia testo e stile in base allo stato)
    private final Button btnAuth = new Button();

    // Nodi che il guest vede ma non può usare (es. "Prenota", "Le mie prenotazioni")
    private final List<Node> nodiRiservati = new ArrayList<>();

    // Stato
    private Utente utenteLoggato;
    private boolean isGuest;

    private final GestoreScene gestoreScene;

    public BaseLayoutController(GestoreScene gestoreScene) {
        this.gestoreScene = gestoreScene;
        costruisciLayout();
    }

    private void costruisciLayout() {
        contenitorePrincipale.getStyleClass().add("sfondo-principale");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));

        Label logo = new Label("CineMax");
        logo.getStyleClass().add("titolo-principale");
        logo.setStyle("-fx-font-size: 22px;");

        labelBenvenuto.getStyleClass().add("testo-secondario");

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);

        btnAuth.setOnAction(this::onAuthCliccato);

        header.getChildren().addAll(logo, labelBenvenuto, spazio, btnAuth);

        // Menu Laterale
        menuLaterale.setPadding(new Insets(20));
        menuLaterale.setAlignment(Pos.TOP_LEFT);
        menuLaterale.setPrefWidth(200);

        contenitorePrincipale.setTop(header);
        contenitorePrincipale.setLeft(menuLaterale);
    }

    public Parent getRoot() {
        return contenitorePrincipale;
    }

    // Vero se il layout sta operando in modalità Guest (utente non autenticato).
    public boolean isGuest() {
        return isGuest;
    }

    public Utente getUtenteLoggato() {
        return utenteLoggato;
    }

    // Inizializza il contesto. utente == null significa Guest.
    public void inizializzaContesto(Utente utente) {
        this.utenteLoggato = utente;
        this.isGuest = (utente == null);

        if (isGuest) {
            labelBenvenuto.setText("Modalità Guest");
        } else {
            labelBenvenuto.setText("Bentornato, " + utente.getNome());
        }

        aggiornaStatoAuth();
        generaMenuLaterale();
    }

    // Inserisce una dashboard al centro senza ricostruire header/menu.
    public void impostaContenutoCentrale(Node contenuto) {
        contenitorePrincipale.setCenter(contenuto);
    }

    // Registra un nodo come "riservato": il guest lo vede attenuato e non lo può cliccare.
    // Da chiamare quando costruisci la dashboard, es: layout.registraNodoRiservato(card.getBottonePrincipale());
    public void registraNodoRiservato(Node n) {
        nodiRiservati.add(n);
        applicaStatoNodo(n);
    }

    // Applica lo stato auth corrente a tutti i nodi riservati e al bottone.
    private void aggiornaStatoAuth() {
        if (isGuest) {
            btnAuth.setText("Log In");
            btnAuth.getStyleClass().setAll("bottone-primario");
        } else {
            btnAuth.setText("Log Out");
            btnAuth.getStyleClass().setAll("bottone-bianco");
        }
        for (Node n : nodiRiservati) {
            applicaStatoNodo(n);
        }
    }

    // Attenua + disattiva il click se guest, ripristina se client.
    private void applicaStatoNodo(Node n) {
        if (isGuest) {
            n.setMouseTransparent(true);
            if (!n.getStyleClass().contains("bloccato-guest")) {
                n.getStyleClass().add("bloccato-guest");
            }
        } else {
            n.setMouseTransparent(false);
            n.getStyleClass().remove("bloccato-guest");
        }
    }

    private void generaMenuLaterale() {
        menuLaterale.getChildren().clear();
        // TODO: switch sul ruolo (Cliente / Proiezionista / Bigliettaio).
        // Per ora si mostra il menu del Cliente, che è anche quello del Guest:
        // le voci riservate vengono registrate via registraNodoRiservato(...).

        Button voceCerca = costruisciVoceMenu("Cerca proiezioni");
        // La ricerca proiezioni è sempre disponibile (anche al Guest), nessun blocco.
        voceCerca.setOnAction(e -> {
            // Già nella dashboard di ricerca: per ora nessuna navigazione aggiuntiva.
            // TODO: se in futuro ci saranno più viste, qui si torna a quella di ricerca.
        });

        Button vocePrenotazioni = costruisciVoceMenu("Le mie prenotazioni");
        vocePrenotazioni.setOnAction(e -> {
            // NAVIGAZIONE (solo clienti registrati)
            // TODO: caricare la dashboard MiePrenotazioni nell'area centrale.
            //   impostaContenutoCentrale(miePrenotazioni.getRoot());
        });
        // "Le mie prenotazioni" è riservata: il Guest la vede attenuata e non cliccabile.
        registraNodoRiservato(vocePrenotazioni);

        menuLaterale.getChildren().addAll(voceCerca, vocePrenotazioni);
    }

    // Costruisce una voce di menu laterale con lo stile a tutta larghezza.
    private Button costruisciVoceMenu(String testo) {
        Button b = new Button(testo);
        b.getStyleClass().add("voce-menu");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    // Gestisce sia Login (guest) che Logout (client).
    private void onAuthCliccato(ActionEvent e) {
        if (isGuest) {
            gestoreScene.vaiALogin();
        } else {
            this.utenteLoggato = null;
            gestoreScene.vaiAStart();
        }
    }
}
