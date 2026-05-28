package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Utente;
import org.springframework.stereotype.Component;

/**
 * Factory concreta per la creazione di utenti con ruolo AZIENDA.
 */
@Component
public class AziendaFactory implements UtenteFactory {

    @Override
    public Utente creaUtente() {
        Azienda azienda = new Azienda();
        azienda.setRuolo(Utente.Ruolo.AZIENDA);
        return azienda;
    }

    @Override
    public Utente.Ruolo getRuoloSupportato() {
        return Utente.Ruolo.AZIENDA;
    }
}
