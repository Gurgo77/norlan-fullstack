package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import it.norlan.clientportal.repository.NotificaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementazione concreta del Design Pattern "Strategy" per il canale IN_APP.
 * Gestisce la persistenza relazionale dei messaggi destinati alla dashboard interna dell'utente,
 * delegando il salvataggio al layer di Data Access.
 */

@Component
public class InAppNotificaStrategy implements NotificaStrategy {

    @Autowired
    private NotificaRepository notificaRepository;

    // Persiste fisicamente la notifica sul database rendendola immediatamente fruibile alla successiva richiesta HTTP del frontend
    @Override
    public void invia(Notifica notifica) {
        notificaRepository.save(notifica);
        System.out.println("[IN-APP] Notifica salvata nel database per l'utente " + notifica.getDestinatario().getEmail());
    }

    // Espone la chiave di routing (Routing Key) al Context del pattern Strategy per la risoluzione dinamica dell'algoritmo
    @Override
    public CanaleNotifica getCanaleSupportato() {
        return CanaleNotifica.IN_APP;
    }
}
