package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Implementazione concreta del Design Pattern "Strategy" per il canale EMAIL.
 * Incapsula la logica di interfacciamento con il server SMTP (JavaMailSender)
 * e sfrutta il motore Thymeleaf per il rendering dinamico (Server-Side) dei template HTML.
 */

@Component
public class EmailNotificaStrategy implements NotificaStrategy {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private LogSincronizzazioneService logService;

    @Value("${spring.mail.username}")
    private String mittente;

    // Analizza il payload (intercettando marcatori custom come "[CHAT]"), compila il contesto del template HTML e assembla il MimeMessage per l'inoltro
    @Override
    public void invia(Notifica notifica) {
        String emailDestinatario = notifica.getDestinatario().getEmail();

        try {
            Context context = new Context();
            String payload = notifica.getMessaggio();
            String htmlBody;
            String subject;

            if (payload != null && payload.startsWith("[CHAT]")) {

                String data = payload.substring("[CHAT]".length());
                int separatorIndex = data.indexOf("|");
                String mittenteEmail = "Sistema";
                String contenutoMessaggio = data;

                if (separatorIndex != -1) {
                    mittenteEmail = data.substring(0, separatorIndex);
                    contenutoMessaggio = data.substring(separatorIndex + 1);
                }

                context.setVariable("mittenteEmail", mittenteEmail);
                context.setVariable("contenutoMessaggio", contenutoMessaggio);
                htmlBody = templateEngine.process("email/messaggio-chat", context);
                subject = "NorLan Portal - Nuovo Messaggio in Chat";

            } else {

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
            System.out.println("[EMAIL HTML] Inviata con successo a: " + emailDestinatario);

            // Gestisce la Fault Tolerance: intercetta i fallimenti di rete o del server SMTP registrando tempestivamente l'anomalia nel sistema di Auditing
        } catch (Exception e) {
            String erroreDettagliato = e.getMessage();
            if (e.getCause() != null) {
                erroreDettagliato += " - Causa: " + e.getCause().getMessage();
            }

            logService.registraEvento(
                    "Fallimento invio notifica Email",
                    false,
                    "Impossibile contattare il server SMTP per l'utente " + emailDestinatario + ". Dettaglio eccezione: " + erroreDettagliato
            );
            e.printStackTrace();
        }
    }

    // Espone la chiave di routing (Routing Key) al Context del pattern Strategy per la selezione e risoluzione dinamica dell'algoritmo a runtime
    @Override
    public CanaleNotifica getCanaleSupportato() {
        return CanaleNotifica.EMAIL;
    }
}
