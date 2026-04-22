package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificaStrategy implements NotificaStrategy {

    // Iniettare  @Autowired JavaMailSender per generazione mail

    @Override
    public void invia(Notifica notifica) {
        String emailDestinatario = notifica.getDestinatario().getEmail();
        String corpo = notifica.getMessaggio();

        System.out.println("📧 [EMAIL] Simulazione invio email a: " + emailDestinatario + " | Testo: " + corpo);
    }

    @Override
    public CanaleNotifica getCanaleSupportato() {
        return CanaleNotifica.EMAIL;
    }
}
