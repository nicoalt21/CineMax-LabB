package cinemax.client.gui.component.overlay;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/*
 Overlay di conferma riutilizzabile, costruito interamente in codice Java.

 E' uno strato a tutta superficie (StackPane con sfondo scurito) che centra un riquadro
 con un messaggio e due bottoni. Estratto da BaseLayoutController per alleggerirlo: il
 layout ne tiene un'istanza, la sovrappone al contenuto e delega qui la logica.

 L'overlay non conosce ne' lo stato utente ne' la navigazione: si limita a mostrare un
 messaggio e due scelte, eseguendo le azioni (Runnable) fornite dal chiamante. Questo lo
 rende del tutto indipendente e riusabile.

 Uso tipico dal layout:
   overlay.mostraConferma("Sei sicuro?", () -> faiQualcosa());
   overlay.mostraScelta("...", "Accedi", "Registrati", azioneA, azioneB);
 */
public class OverlayConferma extends StackPane {

    private final Label labelMessaggio = new Label();
    private final Button btnSi = new Button("Sì");
    private final Button btnNo = new Button("No");

    public OverlayConferma() {
        getStyleClass().add("overlay-conferma");

        VBox riquadro = new VBox(20);
        riquadro.getStyleClass().add("riquadro-conferma");
        riquadro.setAlignment(Pos.CENTER);
        riquadro.setMaxWidth(380);
        riquadro.setMaxHeight(Region.USE_PREF_SIZE);
        riquadro.setPadding(new Insets(25));

        labelMessaggio.getStyleClass().add("testo-normale");
        labelMessaggio.setWrapText(true);
        labelMessaggio.setAlignment(Pos.CENTER);

        btnSi.getStyleClass().add("bottone-primario");
        btnNo.getStyleClass().add("bottone-secondario");
        HBox rigaBottoni = new HBox(12, btnSi, btnNo);
        rigaBottoni.setAlignment(Pos.CENTER);

        riquadro.getChildren().addAll(labelMessaggio, rigaBottoni);
        getChildren().add(riquadro);

        // Nascosto di default.
        setVisible(false);
        setManaged(false);
    }

    /*
     Mostra l'overlay con il messaggio dato e due bottoni "Sì"/"No". Se l'utente preme
     "Sì" viene eseguita azioneSuConferma; in entrambi i casi l'overlay si chiude.
     */
    public void mostraConferma(String messaggio, Runnable azioneSuConferma) {
        mostraScelta(messaggio, "Sì", "No", azioneSuConferma, null);
    }

    /*
     Variante generale: messaggio + due bottoni con etichette e azioni personalizzabili.
     Passare null a un'azione la rende un semplice "chiudi".
     */
    public void mostraScelta(String messaggio, String testoSi, String testoNo,
                             Runnable azioneSi, Runnable azioneNo) {
        labelMessaggio.setText(messaggio);
        btnSi.setText(testoSi);
        btnNo.setText(testoNo);
        btnSi.setOnAction(e -> {
            chiudi();
            if (azioneSi != null) {
                azioneSi.run();
            }
        });
        btnNo.setOnAction(e -> {
            chiudi();
            if (azioneNo != null) {
                azioneNo.run();
            }
        });
        setVisible(true);
        setManaged(true);
    }

    // Nasconde l'overlay.
    public void chiudi() {
        setVisible(false);
        setManaged(false);
    }
}
