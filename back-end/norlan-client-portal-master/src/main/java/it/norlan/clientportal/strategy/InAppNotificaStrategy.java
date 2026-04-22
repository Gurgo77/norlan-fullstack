package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import it.norlan.clientportal.repository.NotificaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificaStrategy implements NotificaStrategy {

    @Autowired
    private NotificaRepository notificaRepository;

    @Override
    public void invia(Notifica notifica) {
        notificaRepository.save(notifica);
        System.out.println("✅ [IN-APP] Notifica salvata nel database per l'utente " + notifica.getDestinatario().getEmail());
    }

    @Override
    public CanaleNotifica getCanaleSupportato() {
        return CanaleNotifica.IN_APP;
    }
}
