/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.gui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.function.IntConsumer;

/*
 Barra di paginazione riutilizzabile, costruita interamente in codice Java.

 Mostra i numeri di pagina nella forma:
     1 ... Corr-1 Corr Corr+1 ... Ultima
 con queste regole (come da specifiche del progetto):
   - se Corr-1 coincide con 1, non si mostrano "..." e Corr-1: resta solo "1";
   - se Corr+1 coincide con l'ultima, non si mostrano Corr+1 e "...": resta solo l'ultima;
   - "1" e l'ultima pagina sono sempre visibili (salvo coincidere con i vicini).

 La pagina corrente, al centro, è cliccabile: diventa un campo di testo inline in cui
 l'utente scrive il numero a cui saltare (Invio conferma, Esc annulla). I "..." sono
 cliccabili e aprono lo stesso campo, come scorciatoia per il salto.

 Il cambio pagina viene notificato al controller tramite il listener registrato con
 setListenerCambioPagina(...); la barra non conosce i dati, si limita a dire "vai alla
 pagina N".
 */
public class BarraPaginazione extends HBox {

    private int paginaCorrente = 1;
    private int numeroPagine = 1;

    // Notifica al controller la pagina scelta (1-based).
    private IntConsumer listenerCambioPagina;

    public BarraPaginazione() {
        super(6);
        setAlignment(Pos.CENTER);
        getStyleClass().add("barra-paginazione");
    }

    // Registra il listener chiamato quando l'utente sceglie una nuova pagina.
    public void setListenerCambioPagina(IntConsumer listener) {
        this.listenerCambioPagina = listener;
    }

    /*
     Aggiorna la barra con la pagina corrente e il numero totale di pagine, poi
     ricostruisce i pulsanti. Con una sola pagina la barra resta vuota (nascosta).
     */
    public void aggiorna(int paginaCorrente, int numeroPagine) {
        this.paginaCorrente = paginaCorrente;
        this.numeroPagine = numeroPagine;
        ricostruisci();
    }

    private void ricostruisci() {
        getChildren().clear();

        // Con 0 o 1 pagina non serve la barra.
        if (numeroPagine <= 1) {
            setVisible(false);
            setManaged(false);
            return;
        }
        setVisible(true);
        setManaged(true);

        // Prima pagina (sempre presente).
        getChildren().add(bottonePagina(1));

        // Tratto sinistro: "..." e Corr-1, con le eccezioni richieste.
        // Corr-1 si mostra solo se è > 1 (altrimenti coincide con la prima).
        if (paginaCorrente - 1 > 1) {
            // "..." solo se c'è un buco fra 1 e Corr-1 (cioè Corr-1 > 2).
            if (paginaCorrente - 1 > 2) {
                getChildren().add(puntini());
            }
            getChildren().add(bottonePagina(paginaCorrente - 1));
        }

        // Pagina corrente al centro (cliccabile -> campo di salto), se non è 1 né ultima.
        if (paginaCorrente != 1 && paginaCorrente != numeroPagine) {
            getChildren().add(etichettaCorrente());
        }

        // Tratto destro: Corr+1 e "...", con le eccezioni richieste.
        // Corr+1 si mostra solo se è < ultima (altrimenti coincide con l'ultima).
        if (paginaCorrente + 1 < numeroPagine) {
            getChildren().add(bottonePagina(paginaCorrente + 1));
            // "..." solo se c'è un buco fra Corr+1 e l'ultima (cioè Corr+1 < ultima-1).
            if (paginaCorrente + 1 < numeroPagine - 1) {
                getChildren().add(puntini());
            }
        }

        // Ultima pagina (sempre presente, se diversa dalla prima).
        if (numeroPagine > 1) {
            getChildren().add(bottonePagina(numeroPagine));
        }
    }

    // Bottone di una pagina specifica. La pagina corrente è evidenziata.
    private Button bottonePagina(int pagina) {
        Button b = new Button(String.valueOf(pagina));
        b.getStyleClass().add("bottone-pagina");
        if (pagina == paginaCorrente) {
            b.getStyleClass().add("bottone-pagina-attiva");
        }
        b.setOnAction(e -> vaiAPagina(pagina));
        return b;
    }

    // L'etichetta della pagina corrente: cliccandola diventa un campo per saltare.
    private Button etichettaCorrente() {
        Button b = new Button(String.valueOf(paginaCorrente));
        b.getStyleClass().addAll("bottone-pagina", "bottone-pagina-attiva");
        b.setOnAction(e -> apriCampoSalto(b));
        return b;
    }

    // I "...": cliccabili, aprono il campo di salto (scorciatoia).
    private Label puntini() {
        Label l = new Label("...");
        l.getStyleClass().add("puntini-pagina");
        l.setOnMouseClicked(e -> apriCampoSaltoDaPuntini(l));
        return l;
    }

    // Sostituisce un nodo con un campo di testo per inserire la pagina di destinazione.
    private void apriCampoSalto(Button origine) {
        int indice = getChildren().indexOf(origine);
        if (indice < 0) {
            return;
        }
        TextField campo = campoSalto();
        getChildren().set(indice, campo);
        campo.requestFocus();
    }

    private void apriCampoSaltoDaPuntini(Label origine) {
        int indice = getChildren().indexOf(origine);
        if (indice < 0) {
            return;
        }
        TextField campo = campoSalto();
        getChildren().set(indice, campo);
        campo.requestFocus();
    }

    // Crea il campo di salto: Invio conferma il numero, Esc o perdita di focus annulla.
    private TextField campoSalto() {
        TextField campo = new TextField(String.valueOf(paginaCorrente));
        campo.getStyleClass().add("campo-salto-pagina");
        campo.setPrefColumnCount(3);
        campo.setOnAction(e -> confermaSalto(campo.getText()));
        campo.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                ricostruisci(); // annulla: ripristina la barra
            }
        });
        campo.focusedProperty().addListener((obs, prima, dopo) -> {
            if (!dopo) {
                ricostruisci(); // se perde il focus senza confermare, ripristina
            }
        });
        return campo;
    }

    // Valida il numero inserito e, se valido, salta alla pagina.
    private void confermaSalto(String testo) {
        try {
            int pagina = Integer.parseInt(testo.trim());
            if (pagina >= 1 && pagina <= numeroPagine) {
                vaiAPagina(pagina);
                return;
            }
        } catch (NumberFormatException ignored) {
            // input non valido: cadiamo nel ripristino sotto
        }
        ricostruisci(); // numero fuori range o non valido: ripristina senza saltare
    }

    private void vaiAPagina(int pagina) {
        if (pagina == paginaCorrente) {
            ricostruisci();
            return;
        }
        if (listenerCambioPagina != null) {
            listenerCambioPagina.accept(pagina);
        }
    }
}
