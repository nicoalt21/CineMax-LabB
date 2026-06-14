package cinemax.client.gui.component;

import cinemax.common.model.Prenotazione;
import java.util.function.Consumer;

public class CardPrenotazione {

    //Gestisce card_prenotazione.fxml
    public void compilaDatiPrenotazione(Prenotazione p, String ruoloUtente){};

    public void setAzioneModifica(Consumer<Prenotazione> azione){};

    public void setAzioneElimina(Consumer<Prenotazione> azione){};


}
