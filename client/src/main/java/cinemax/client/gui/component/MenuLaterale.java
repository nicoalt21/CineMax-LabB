package cinemax.client.gui.component;

import cinemax.client.controller.shared.BaseLayoutController;
import cinemax.common.model.Utente;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Componente che gestisce il menu laterale dell'applicazione.
 * Si occupa di generare le voci di menu in base al ruolo dell'utente
 * (Cliente, Proiezionista, Bigliettaio o Guest) e di gestire l'espansione
 * e la compressione (collassamento) del pannello.
 *
 * @author Alt Niccolò Jacopo, 762605, VA
 * @author Gerti, Alessia, 762405, VA
 * @author Soldo Mateo, 760762, VA
 * @author Vignati Davide, 761134, VA
 */
public class MenuLaterale extends HBox {

    private final VBox menuLateraleBox = new VBox(10);
    private final VBox contenutoVoci = new VBox(10);
    private final Button btnCollassaMenu = new Button("<");
    private final HBox pannelloMenu = new HBox();
    private boolean menuCollassato = false;

    private final BaseLayoutController layout;

    /**
     * Costruisce il menu laterale collegandolo al layout principale.
     *
     * @param layout Il controller del layout radice per gestire la navigazione e lo stato.
     */
    public MenuLaterale(BaseLayoutController layout) {
        this.layout = layout;
        costruisciInterfaccia();
    }

    private void costruisciInterfaccia() {
        setAlignment(Pos.TOP_LEFT);

        btnCollassaMenu.getStyleClass().add("bottone-collassa-menu");
        btnCollassaMenu.setOnAction(e -> toggleMenuLaterale());

        Region spintaToggle = new Region();
        HBox.setHgrow(spintaToggle, Priority.ALWAYS);
        HBox rigaToggle = new HBox(spintaToggle, btnCollassaMenu);
        rigaToggle.setAlignment(Pos.TOP_RIGHT);

        menuLateraleBox.setPadding(new Insets(10, 12, 20, 12));
        menuLateraleBox.setAlignment(Pos.TOP_LEFT);
        menuLateraleBox.setPrefWidth(200);

        HBox.setHgrow(menuLateraleBox, Priority.ALWAYS);
        VBox.setVgrow(contenutoVoci, Priority.ALWAYS);

        menuLateraleBox.getChildren().addAll(rigaToggle, contenutoVoci);
        pannelloMenu.getChildren().add(menuLateraleBox);

        Separator divisoreLaterale = new Separator(Orientation.VERTICAL);
        divisoreLaterale.getStyleClass().add("divisore-menu");

        getChildren().addAll(pannelloMenu, divisoreLaterale);
    }

    /**
     * Aggiorna le voci del menu in base all'utente loggato.
     */
    public void generaMenu() {
        contenutoVoci.getChildren().clear();
        VBox vociAlto = new VBox(10);

        Utente utente = layout.getUtenteLoggato();
        boolean isGuest = layout.isGuest();

        if (isGuest || utente.getRuolo() == cinemax.common.model.Ruolo.CLIENTE) {
            Button voceCerca = costruisciVoceMenu("Cerca proiezioni");
            voceCerca.setOnAction(e -> layout.mostraDashboardRicerca());

            Button vocePrenotazioni = costruisciVoceMenu("Le mie prenotazioni");
            vocePrenotazioni.setOnAction(e -> layout.mostraMiePrenotazioni());
            layout.registraNodoRiservato(vocePrenotazioni); // riservata: Guest non prenota

            Button voceProfilo = costruisciVoceMenu("Profilo");
            voceProfilo.setOnAction(e -> layout.mostraProfilo());

            Button voceImpostazioni = costruisciVoceMenu("Impostazioni");
            voceImpostazioni.setOnAction(e -> layout.mostraImpostazioni());

            vociAlto.getChildren().addAll(voceCerca, vocePrenotazioni, voceProfilo, voceImpostazioni);

        } else if (utente.getRuolo() == cinemax.common.model.Ruolo.PROIEZIONISTA) {
            Button voceGestione = costruisciVoceMenu("Gestione proiezioni");
            voceGestione.setOnAction(e -> layout.mostraGestioneProiezioni());

            Button voceCrea = costruisciVoceMenu("Crea proiezione");
            voceCrea.setOnAction(e -> layout.mostraCreaProiezione());

            Button voceCreaFilm = costruisciVoceMenu("Crea film");
            voceCreaFilm.setOnAction(e -> layout.mostraCreaFilm());

            Button voceProfilo = costruisciVoceMenu("Profilo");
            voceProfilo.setOnAction(e -> layout.mostraProfilo());

            Button voceImpostazioni = costruisciVoceMenu("Impostazioni");
            voceImpostazioni.setOnAction(e -> layout.mostraImpostazioni());

            vociAlto.getChildren().addAll(voceGestione, voceCrea, voceCreaFilm, voceProfilo, voceImpostazioni);

        } else {
            Button voceOggi = costruisciVoceMenu("Proiezioni di oggi");
            voceOggi.setOnAction(e -> layout.mostraProiezioniOggi());

            Button voceVerifica = costruisciVoceMenu("Verifica biglietto");
            voceVerifica.setOnAction(e -> layout.mostraVerificaBiglietto());

            Button voceProfilo = costruisciVoceMenu("Profilo");
            voceProfilo.setOnAction(e -> layout.mostraProfilo());

            Button voceImpostazioni = costruisciVoceMenu("Impostazioni");
            voceImpostazioni.setOnAction(e -> layout.mostraImpostazioni());

            vociAlto.getChildren().addAll(voceOggi, voceVerifica, voceProfilo, voceImpostazioni);
        }

        Region spinta = new Region();
        VBox.setVgrow(spinta, Priority.ALWAYS);

        Separator divisoreRuolo = new Separator(Orientation.HORIZONTAL);
        divisoreRuolo.getStyleClass().add("divisore-menu");

        Label etichettaRuolo = new Label(descrizioneRuolo(isGuest, utente));
        etichettaRuolo.getStyleClass().add("etichetta-ruolo");
        etichettaRuolo.setMaxWidth(Double.MAX_VALUE);
        etichettaRuolo.setAlignment(Pos.CENTER);

        contenutoVoci.getChildren().addAll(vociAlto, spinta, divisoreRuolo, etichettaRuolo);
    }

    private Button costruisciVoceMenu(String testo) {
        Button b = new Button(testo);
        b.getStyleClass().add("voce-menu");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    private void toggleMenuLaterale() {
        menuCollassato = !menuCollassato;
        contenutoVoci.setVisible(!menuCollassato);
        contenutoVoci.setManaged(!menuCollassato);
        btnCollassaMenu.setText(menuCollassato ? ">" : "<");

        if (menuCollassato) {
            menuLateraleBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
            menuLateraleBox.setMinWidth(Region.USE_COMPUTED_SIZE);
            menuLateraleBox.setMaxWidth(Region.USE_COMPUTED_SIZE);
        } else {
            menuLateraleBox.setPrefWidth(200);
            menuLateraleBox.setMinWidth(Region.USE_COMPUTED_SIZE);
            menuLateraleBox.setMaxWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    private String descrizioneRuolo(boolean isGuest, Utente utente) {
        if (isGuest) return "Modalità Guest";
        switch (utente.getRuolo()) {
            case PROIEZIONISTA: return "Proiezionista";
            case BIGLIETTAIO:   return "Bigliettaio";
            default:            return "Cliente";
        }
    }
}