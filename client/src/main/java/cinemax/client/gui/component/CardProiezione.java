/*
 * Autore: (compilare) - matricola: (compilare) - sede: VA/CO
 */
package cinemax.client.gui.component;

import cinemax.client.gui.util.FasciaEta;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/*
 Scheda (card) che mostra i dati di una singola Proiezione, costruita interamente in
 codice Java (niente FXML, come il resto della UI).

 Layout compatto: titolo (con pallino del limite d'età accanto), una riga di dettagli
 film, una riga con data/prezzo/posti e la riga azioni.

 Il pallino d'età usa un semaforo (verde/giallo/arancione/rosso) calcolato da
 FasciaEta in base all'età minima del film. Accanto al pallino è mostrato il valore
 numerico (es. "VM13").

 Riutilizzo tra ruoli: il bottone azione principale cambia etichetta e comportamento a
 seconda del ruolo (es. "Prenota" per il cliente, "Modifica" per il proiezionista). Le
 azioni vengono iniettate dal controller padre tramite setAzionePrincipale(...) e
 setAzioneSecondaria(...), così la stessa card serve dashboard diverse.
 */
public class CardProiezione extends VBox {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final Label titoloLabel = new Label();
    private final Circle pallinoEta = new Circle(6);
    private final Label etaLabel = new Label();
    private final Label dettagliFilmLabel = new Label();
    private final Label dataLabel = new Label();
    private final Label prezzoLabel = new Label();
    private final Label postiLabel = new Label();

    private final Button bottonePrincipale = new Button();
    private final Button bottoneSecondario = new Button();

    // Proiezione attualmente mostrata, passata alle azioni come argomento.
    private Proiezione proiezioneCorrente;

    public CardProiezione() {
        super(2);
        setPadding(new Insets(7, 14, 7, 14));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("card-proiezione");

        titoloLabel.getStyleClass().add("card-titolo");
        titoloLabel.setWrapText(true);

        etaLabel.getStyleClass().add("testo-secondario");

        // Titolo + pallino età + valore numerico, sulla stessa riga.
        Region spazioTitolo = new Region();
        HBox.setHgrow(spazioTitolo, Priority.ALWAYS);
        HBox rigaTitolo = new HBox(8, titoloLabel, spazioTitolo, pallinoEta, etaLabel);
        rigaTitolo.setAlignment(Pos.CENTER_LEFT);

        dettagliFilmLabel.getStyleClass().add("testo-secondario");
        dettagliFilmLabel.setWrapText(true);

        dataLabel.getStyleClass().add("testo-normale");
        prezzoLabel.getStyleClass().add("testo-normale");
        postiLabel.getStyleClass().add("testo-normale");

        bottonePrincipale.getStyleClass().add("bottone-primario");
        bottoneSecondario.getStyleClass().add("bottone-secondario");

        // I bottoni sono nascosti finchè non viene registrata un'azione per loro.
        bottonePrincipale.setManaged(false);
        bottonePrincipale.setVisible(false);
        bottoneSecondario.setManaged(false);
        bottoneSecondario.setVisible(false);

        HBox rigaInfo = new HBox(16, dataLabel, prezzoLabel, postiLabel);
        rigaInfo.setAlignment(Pos.CENTER_LEFT);

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);
        HBox rigaAzioni = new HBox(8, spazio, bottoneSecondario, bottonePrincipale);
        rigaAzioni.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(rigaTitolo, dettagliFilmLabel, rigaInfo, rigaAzioni);
    }

    /*
     Popola la card con i dati di una proiezione. Il parametro ruoloUtente serve a
     personalizzare l'etichetta del bottone principale (es. "Prenota" vs "Modifica").
     Passare ruoloUtente null va bene per il Guest: in quel caso si usa l'etichetta di
     default e il controllo accessi (attenuazione/blocco) è gestito dal layout tramite
     registraNodoRiservato(card.getBottonePrincipale()).
     */
    public void compilaDatiProiezione(Proiezione p, Ruolo ruoloUtente) {
        this.proiezioneCorrente = p;

        Film film = p.getFilm();

        titoloLabel.setText(film.getTitolo());

        // Pallino del limite d'età (semaforo) + valore numerico.
        impostaPallinoEta(film.getEtaMinima());

        dettagliFilmLabel.setText(
                film.getGenere()
                        + "  -  " + film.getRegista()
                        + "  -  " + film.getAnno()
                        + "  -  " + film.getDurataMinuti() + " min"
        );

        dataLabel.setText(p.getDataOra().format(FORMATO_DATA));
        prezzoLabel.setText(String.format("%.2f €", p.getCostoBiglietto()));

        int posti = p.getPostiLiberi();
        postiLabel.getStyleClass().remove("testo-esaurito");
        if (posti <= 0) {
            postiLabel.setText("Esaurito");
            postiLabel.getStyleClass().add("testo-esaurito");
        } else {
            postiLabel.setText(posti + (posti == 1 ? " posto libero" : " posti liberi"));
        }

        // Etichetta del bottone principale in base al ruolo.
        if (ruoloUtente == Ruolo.PROIEZIONISTA) {
            bottonePrincipale.setText("Modifica");
        } else {
            bottonePrincipale.setText("Prenota");
        }
    }

    // Colora il pallino in base alla fascia d'età e mostra il valore (es. "VM13").
    private void impostaPallinoEta(int etaMinima) {
        FasciaEta.Fascia fascia = FasciaEta.fasciaPerEta(etaMinima);
        pallinoEta.getStyleClass().setAll("pallino-eta", fascia.getClasseCss());
        etaLabel.setText("VM" + etaMinima);
        Tooltip.install(pallinoEta, new Tooltip("Vietato ai minori di " + etaMinima + " anni"));
    }

    /*
     Blocca l'azione di prenotazione: disabilita il bottone principale e mostra il
     motivo come tooltip. Usato quando l'utente non ha l'età minima per il film.
     */
    public void bloccaPrenotazione(String motivo) {
        bottonePrincipale.setDisable(true);
        Tooltip.install(bottonePrincipale, new Tooltip(motivo));
    }

    /*
     Registra l'azione del bottone principale (es. "Prenota" per il cliente). Il
     Consumer riceve la Proiezione mostrata da questa card. Registrare un'azione rende
     il bottone visibile.
     */
    public void setAzionePrincipale(Consumer<Proiezione> azione) {
        bottonePrincipale.setManaged(true);
        bottonePrincipale.setVisible(true);
        bottonePrincipale.setOnAction(e -> {
            if (azione != null && proiezioneCorrente != null) {
                azione.accept(proiezioneCorrente);
            }
        });
    }

    /*
     Registra l'azione del bottone secondario (es. "Elimina" per il proiezionista).
     Registrare un'azione rende il bottone visibile.
     */
    public void setAzioneSecondaria(Consumer<Proiezione> azione) {
        bottoneSecondario.setManaged(true);
        bottoneSecondario.setVisible(true);
        bottoneSecondario.setText("Elimina");
        bottoneSecondario.setOnAction(e -> {
            if (azione != null && proiezioneCorrente != null) {
                azione.accept(proiezioneCorrente);
            }
        });
    }

    // Permette al controller padre di personalizzare l'etichetta del bottone principale.
    public void setEtichettaPrincipale(String testo) {
        bottonePrincipale.setText(testo);
    }

    /*
     Espone il bottone principale per consentire al BaseLayoutController di registrarlo
     come "nodo riservato": per il Guest verrà attenuato e reso non cliccabile.
     Es: layout.registraNodoRiservato(card.getBottonePrincipale());
     */
    public Button getBottonePrincipale() {
        return bottonePrincipale;
    }

    public Button getBottoneSecondario() {
        return bottoneSecondario;
    }

    public Proiezione getProiezione() {
        return proiezioneCorrente;
    }
}
