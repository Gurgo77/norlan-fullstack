package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.*;
import org.springframework.stereotype.Component;

@Component
public class UtenteFactory {
    public Utente creaUtente(Utente.Ruolo ruolo) {
        if (ruolo == null) {
            throw new IllegalArgumentException("Impossibile istanziare un Utente: il ruolo non può essere nullo.");
        }

        switch (ruolo) {
            case ADMIN:
                Admin admin = new Admin();
                admin.setRuolo(Utente.Ruolo.ADMIN);
                return admin;

            case DOCENTE:
                Docente docente = new Docente();
                docente.setRuolo(Utente.Ruolo.DOCENTE);
                return docente;

            case AZIENDA:
                Azienda azienda = new Azienda();
                azienda.setRuolo(Utente.Ruolo.AZIENDA);
                return azienda;

            case DIPENDENTE:
                Dipendente dipendente = new Dipendente();
                dipendente.setRuolo(Utente.Ruolo.DIPENDENTE);
                return dipendente;

            default:
                throw new UnsupportedOperationException("Strategia di istanziazione non implementata per il ruolo: " + ruolo);
        }
    }
}
