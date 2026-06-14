package cinemax.client.gui.component;

import cinemax.common.model.Proiezione;
import java.util.function.Consumer;

public class CardProiezione {

    /*
    Scheda info Proiezione

    Bindings UI:
    Label titleLabel, dateLabel, priceLabel, seatsLabel
    Button primaryActionBtn

    Input: setDataProiezione(Proiezione p, Ruolo ruoloUtente) NB. in pase al ruoloUtente cambia la funzionalità del bottone, "Prenota"/"Modifica" (Così cerchiamo di riciclare codice)

    Output: idk!

     */

    public void compilaDatiProiezione(Proiezione p, String ruoloUtente){};

    public void setAzionePrincipale(Consumer<Proiezione> azione){}; //(Es. per il bottone "Prenota")

    public void setAzioneSecondaria(Consumer<Proiezione> azione){}; //(Es. per il bottone "Elimina")

}
