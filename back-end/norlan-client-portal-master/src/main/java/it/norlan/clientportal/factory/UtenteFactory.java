package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.*;
import org.springframework.stereotype.Component;

/**
 * Implementazione del Design Pattern creazionale "Factory".
 * Centralizza la logica di istanziazione polimorfica delle classi figlie (Admin, Docente, Azienda, Dipendente)
 * in base al ruolo richiesto, disaccoppiando la creazione dell'oggetto dal resto della logica di business.
 */

@Component
public class UtenteFactory {
    // Valida il parametro in ingresso e genera l'istanza specifica della sottoclasse utente corrispondente al ruolo
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
