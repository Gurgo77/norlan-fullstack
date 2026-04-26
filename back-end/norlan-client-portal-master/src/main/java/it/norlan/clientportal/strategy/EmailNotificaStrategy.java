package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class EmailNotificaStrategy implements NotificaStrategy {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String mittente;

    @Override
    public void invia(Notifica notifica) {
        String emailDestinatario = notifica.getDestinatario().getEmail();

        try {
            Context context = new Context();
            String payload = notifica.getMessaggio();
            String htmlBody;
            String subject;

            // CONTROLLO TEMPLATE: Se il messaggio inizia con il marker della chat
            if (payload != null && payload.startsWith("")) {

                // Rimuoviamo il marker per ottenere solo i dati (email|testo)
                String data = payload.replace("", "");
                int separatorIndex = data.indexOf("|");

                String mittenteEmail = "Sistema";
                String contenutoMessaggio = data;

                // Dividiamo la stringa: a sinistra l'email, a destra il testo del messaggio
                if (separatorIndex != -1) {
                    mittenteEmail = data.substring(0, separatorIndex);
                    contenutoMessaggio = data.substring(separatorIndex + 1);
                }

                // Iniettiamo i dati specifici per il template della chat
                context.setVariable("mittenteEmail", mittenteEmail);
                context.setVariable("contenutoMessaggio", contenutoMessaggio);

                htmlBody = templateEngine.process("email/messaggio-chat", context);
                subject = "NorLan Portal - Nuovo Messaggio in Chat";

            } else {
                // COMPORTAMENTO STANDARD: Per tutte le altre notifiche
                context.setVariable("messaggioCorpo", payload);
                htmlBody = templateEngine.process("email/notifica", context);
                subject = "Norlan - Aggiornamento Importante";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mittente);
            helper.setTo(emailDestinatario);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("📧 [EMAIL HTML] Inviata con successo a: " + emailDestinatario);

        } catch (Exception e) {
            System.err.println("❌ [EMAIL HTML] Errore critico durante l'invio a: " + emailDestinatario);
            e.printStackTrace();
        }
    }

    @Override
    public CanaleNotifica getCanaleSupportato() {
        return CanaleNotifica.EMAIL;
    }
}
