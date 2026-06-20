package cinemax.client.gui.component;

import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;
import cinemax.common.model.Ruolo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/*
 Scheda (card) che mostra i dati di una singola Proiezione, costruita interamente in
 codice Java (niente FXML, come il resto della UI).

 I dettagli del film vengono letti dall'oggetto Film associato alla proiezione
 (Proiezione.getFilm()), mentre data/ora, costo e posti liberi appartengono alla
 Proiezione stessa.

 Riutilizzo tra ruoli: il bottone azione principale cambia etichetta e comportamento a
 seconda del ruolo (es. "Prenota" per il cliente, "Modifica" per il proiezionista). Le
 azioni vengono iniettate dal controller padre tramite setAzionePrincipale(...) e
 setAzioneSecondaria(...), così la stessa card serve dashboard diverse.
 */
public class CardProiezione extends VBox {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final Label titoloLabel = new Label();
    private final Label dettagliFilmLabel = new Label();
    private final Label dataLabel = new Label();
    private final Label prezzoLabel = new Label();
    private final Label postiLabel = new Label();

    private final Button bottonePrincipale = new Button();
    private final Button bottoneSecondario = new Button();

    // Proiezione attualmente mostrata, passata alle azioni come argomento.
    private Proiezione proiezioneCorrente;

    public CardProiezione() {
        super(8);
        setPadding(new Insets(15));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("card-proiezione");

        titoloLabel.getStyleClass().add("card-titolo");
        titoloLabel.setWrapText(true);

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

        HBox rigaInfo = new HBox(20, dataLabel, prezzoLabel, postiLabel);
        rigaInfo.setAlignment(Pos.CENTER_LEFT);

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);
        HBox rigaAzioni = new HBox(10, spazio, bottoneSecondario, bottonePrincipale);
        rigaAzioni.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(titoloLabel, dettagliFilmLabel, rigaInfo, rigaAzioni);
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

        dettagliFilmLabel.setText(
                film.getGenere()
                        + "  -  " + film.getRegista()
                        + "  -  " + film.getAnno()
                        + "  -  " + film.getDurataMinuti() + " min"
                        + "  -  VM" + film.getEtaMinima()
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
