package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.model.Utente;
import org.springframework.stereotype.Component;

/**
 * Factory concreta per la creazione di utenti con ruolo DIPENDENTE.
 */
@Component
public class DipendenteFactory implements UtenteFactory {

    @Override
    public Utente creaUtente() {
        Dipendente dipendente = new Dipendente();
        dipendente.setRuolo(Utente.Ruolo.DIPENDENTE);
        return dipendente;
    }

    @Override
    public Utente.Ruolo getRuoloSupportato() {
        return Utente.Ruolo.DIPENDENTE;
    }
}
