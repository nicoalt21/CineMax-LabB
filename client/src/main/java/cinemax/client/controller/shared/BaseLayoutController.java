package cinemax.client.controller.shared;

import cinemax.common.model.Utente;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/*
 Controller per il layout principale dell'applicazione (base_layout.fxml). Funge da guscio per tutte le dashboard.
 Contiene una barra superiore, un menu laterale dinamico e un'area centrale in cui vengono caricate le varie schermate.
 */
public class BaseLayoutController {

    /* RIFERIMENTI UI (@FXML) collegati al file base_layout.fxml
    BorderPane mainContainer
    Vbox sideMenu
    Button logoutBtn
     */

    // Contenitore radice. Il menu andrà a sinistra, l'header in alto e le dashboard al centro
    @FXML private BorderPane contenitorePrincipale;

    // Contenitore del menu laterale in cui butteremo dentro i bottoni dinamicamente
    @FXML private VBox menuLaterale;

    // Bottone per fare logout
    @FXML private Button btnLogout;

    // Variabili di Stato
    private Utente utenteLoggato;


    // Metodi Pubblici e SetUp
    public void inizializzaContesto(Utente utente) // TODO: Implementare l'assegnazione e l'aggiornamento UI
    {
        // utente è restituito dal server dopo un login con successo.

        // Metodo chiamato subito dopo il caricamento del file FXML per passare l'utente connesso
        // 1. Salvare l'utente nella variabile di stato
        // 2. Aggiornare il testo della labelBenvenuto.
        // 3. Chiamare il metodo generaMenuLaterale() per costruire i bottoni corretti.
    };

    public void impostaContenutoCentrale(Node contenuto) { // TODO: Implementare contenitorePrincipale.setCenter(contenuto);

        // Il nodo radice del file FXML della dashboard da mostrare.

        // Inserisce un nuovo "nodo" (una pagina/schermata) al centro del layout. Viene usato dal GestoreScene per cambiare pagina senza ricaricare menu e header.
        // Deve assegnare contenuto alla regione centrale del contenitorePrincipale.
    };


    // Metodi Privati
    private void generaMenuLaterale(){ // TODO: Implementare lo switch sul ruolo e la creazione dinamica dei bottoni

        // Genera dinamicamente i bottoni del menu laterale in base al ruolo dell'utenteLoggato.
        // 1. Svuotare eventuali figli già presenti nel menuLaterale.
        // 2. Tramite uno switch (o if/else) sul ruolo dell'utente (Cliente, Proiezionista, Bigliettaio).
        // 3. Per ogni funzionalità associata al ruolo, creare un Button JavaFX.
        // 4. Assegnare al Button un'azione (setOnAction) che dica al GestoreScene di caricare la vista corrispondente.
        // 5. Aggiungere i bottoni creati al menuLaterale.

    };


    // Gestione Eventi (@FXML)
    @FXML public void onLogoutCliccato(ActionEvent e){ // TODO: Implementare la logica di disconessione
        // e evento scatenato dal click del mouse

        // 1. Pulire utenteLoggato (imposta a null)
        // 2. Dire al GestoreScene di tornare alla schermata di Start o Login.
    };




}
