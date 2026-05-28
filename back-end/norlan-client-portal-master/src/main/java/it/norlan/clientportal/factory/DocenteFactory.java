package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.model.Utente;
import org.springframework.stereotype.Component;

/**
 * Factory concreta per la creazione di utenti con ruolo DOCENTE.
 */
@Component
public class DocenteFactory implements UtenteFactory {

    @Override
    public Utente creaUtente() {
        Docente docente = new Docente();
        docente.setRuolo(Utente.Ruolo.DOCENTE);
        return docente;
    }

    @Override
    public Utente.Ruolo getRuoloSupportato() {
        return Utente.Ruolo.DOCENTE;
    }
}
