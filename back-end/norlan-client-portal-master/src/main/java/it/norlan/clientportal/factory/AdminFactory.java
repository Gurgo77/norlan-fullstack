package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Utente;
import org.springframework.stereotype.Component;

/**
 * Factory concreta per la creazione di ADMIN.
 */
@Component
public class AdminFactory implements UtenteFactory {

    @Override
    public Utente creaUtente() {
        Admin admin = new Admin();
        admin.setRuolo(Utente.Ruolo.ADMIN);
        return admin;
    }

    @Override
    public Utente.Ruolo getRuoloSupportato() {
        return Utente.Ruolo.ADMIN;
    }
}
