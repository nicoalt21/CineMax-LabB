package cinemax.client.gui.component;

public class FilterBarComponent {

    /* Gestirà barra_filtri.fmxml

    Avrà come binding UI:
    TextField serchField
    DatePicker fromDate
    Datepicker toDate
    Button searchBtn

    Nessun Input di default previsto, però per sicurezza ci metterò un SetDefaultFilters

    Output: setOnSearch passa al controller padre un pacchetto di dati (classe predefinita SearchRequest?)

    */

    // private void onCercaCliccato(ActionEvent event){};

    public void impostaVisibilitaFiltri(boolean mostraDate, boolean mostraPrezzo){};

    public void setListenerRicerca(Consumer<FiltriRicerca> listener){};

    public void svuotaFiltri(){};

}
