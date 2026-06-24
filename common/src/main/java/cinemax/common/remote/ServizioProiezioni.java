package cinemax.common.remote;

import cinemax.common.model.CriteriRicercaProiezione;
import cinemax.common.model.Film;
import cinemax.common.model.Proiezione;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface ServizioProiezioni extends Remote {

    List<Proiezione> cercaProiezioni(CriteriRicercaProiezione criteri) throws RemoteException;

    Proiezione visualizzaProiezione(int idProiezione) throws RemoteException;

    boolean aggiungiProiezione(Proiezione proiezione) throws RemoteException;

    /*
     ATTENZIONE BACK-END: la firma di questo metodo e' CAMBIATA.
     Prima era: boolean modificaProiezione(int idProiezione, LocalDateTime nuovaDataOra)
     e cambiava solo l'orario.

     Ora la modifica e' completa: una proiezione esistente (stesso idProiezione, che NON
     cambia) puo' cambiare film, data/ora e costo. L'implementazione deve:
       - ripassare gli stessi vincoli d'integrita' di una creazione: nessuna
         sovrapposizione temporale con le ALTRE proiezioni (la proiezione stessa va
         ESCLUSA dal confronto, altrimenti collide con la propria versione precedente);
         la proiezione deve terminare entro le 24:00 (niente scavalco di mezzanotte);
       - NON invalidare le prenotazioni gia' associate alla proiezione (i biglietti
         restano validi sul nuovo orario): conviene mutare l'oggetto esistente in-place
         invece di distruggerlo e ricrearlo;
       - restituire false se i vincoli non sono rispettati o l'id non esiste.
     */
    boolean modificaProiezione(int idProiezione, int idFilm, LocalDateTime nuovaDataOra,
                               double costo) throws RemoteException;

    boolean eliminaProiezione(int idProiezione) throws RemoteException;

    // Crea un nuovo film nel catalogo. Restituisce false se esiste gia' un film IDENTICO
    // su tutti i campi (titolo, genere, regista, anno, durata, eta' minima). Due film con
    // lo stesso titolo ma altri dati diversi (es. un remake) sono ammessi.
    boolean aggiungiFilm(Film film) throws RemoteException;

    // Elenca i film del catalogo (usato, ad esempio, per popolare i selettori).
    List<Film> elencaFilm() throws RemoteException;

    /*
     BACK-END (nuovo metodo): ritorna i film il cui titolo CONTIENE la stringa passata
     (confronto case-insensitive), per i suggerimenti durante la digitazione. L'identita'
     del film resta la chiave primaria idFilm: il titolo serve solo per cercare, non per
     identificare. Stringa vuota/null -> conviene restituire l'intero catalogo o lista vuota,
     a scelta dell'implementazione (il client gestisce entrambi i casi).
     */
    List<Film> cercaFilmPerTitolo(String titoloParziale) throws RemoteException;

    /*
     BACK-END (nuovo metodo): ritorna gli orari d'INIZIO ammessi (multipli di 5 minuti) in
     cui un film della durata indicata puo' iniziare nel giorno dato, senza sovrapporsi ad
     altre proiezioni e terminando entro le 24:00. Regole:
       - la sala e' libera dalle 00:00 e tutte le proiezioni devono finire entro le 24:00;
       - uno slot d'inizio S e' ammesso se l'intervallo [S, S + durataMinuti) non si
         sovrappone a nessun intervallo gia' occupato e S + durataMinuti <= 24:00;
       - le fini delle proiezioni gia' occupate vanno arrotondate per ECCESSO al multiplo
         di 5 minuti successivo, cosi' il prossimo slot libero parte pulito;
       - idProiezioneDaEscludere permette, in fase di MODIFICA, di non considerare la
         proiezione che si sta modificando (cosi' non collide con se stessa). In fase di
         CREAZIONE si passa un id non valido, es. -1.
     */
    List<LocalTime> finestreLibere(LocalDate giorno, int durataMinuti,
                                   int idProiezioneDaEscludere) throws RemoteException;
}
