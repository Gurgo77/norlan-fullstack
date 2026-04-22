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
            context.setVariable("messaggioCorpo", notifica.getMessaggio());

             String htmlBody = templateEngine.process("email/notifica", context);

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mittente);
            helper.setTo(emailDestinatario);
            helper.setSubject("Norlan - Aggiornamento Importante");

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
