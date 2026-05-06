package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificaStrategyTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private EmailNotificaStrategy strategy;

    private Notifica notifica;
    private Utente destinatario;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(strategy, "mittente", "noreply@norlan.it");

        destinatario = new Utente() {};
        destinatario.setEmail("utente@test.it");

        notifica = new Notifica();
        notifica.setDestinatario(destinatario);

        Session session = Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);
    }

    @Test
    void getCanaleSupportato_RitornaEmail() {
        assertEquals(Notifica.CanaleNotifica.EMAIL, strategy.getCanaleSupportato(),
                "La strategia deve dichiarare esplicitamente il supporto per il canale EMAIL.");
    }

    @Test
    void invia_NotificaStandard_ProcessaTemplateCorrettoEInvia() throws Exception {
        notifica.setMessaggio("Attenzione: Scadenza corso imminente");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/notifica"), any(Context.class))).thenReturn("<html>Testo Standard</html>");

        strategy.invia(notifica);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/notifica"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context capturedContext = contextCaptor.getValue();
        assertEquals("Attenzione: Scadenza corso imminente", capturedContext.getVariable("messaggioCorpo"));
        assertEquals("Norlan - Aggiornamento Importante", mimeMessage.getSubject());
        assertEquals("utente@test.it", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    void invia_NotificaChatCompleta_EstraeDatiEInvia() throws Exception {
        notifica.setMessaggio("[CHAT]collega@norlan.it|Ciao, come stai?");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/messaggio-chat"), any(Context.class))).thenReturn("<html>Chat</html>");

        strategy.invia(notifica);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/messaggio-chat"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context capturedContext = contextCaptor.getValue();
        assertEquals("collega@norlan.it", capturedContext.getVariable("mittenteEmail"));
        assertEquals("Ciao, come stai?", capturedContext.getVariable("contenutoMessaggio"));
        assertEquals("NorLan Portal - Nuovo Messaggio in Chat", mimeMessage.getSubject());
    }

    @Test
    void invia_NotificaChatSenzaSeparatore_UsaValoriDiDefault() throws Exception {
        notifica.setMessaggio("[CHAT]Messaggio anonimo senza delimitatore");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/messaggio-chat"), any(Context.class))).thenReturn("<html>Chat Fallback</html>");

        strategy.invia(notifica);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/messaggio-chat"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context capturedContext = contextCaptor.getValue();
        assertEquals("Sistema", capturedContext.getVariable("mittenteEmail"));
        assertEquals("Messaggio anonimo senza delimitatore", capturedContext.getVariable("contenutoMessaggio"));
    }

    @Test
    void invia_ErroreDiRete_RegistraLogSenzaInterrompereEsecuzione() {
        notifica.setMessaggio("Messaggio standard");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/notifica"), any(Context.class))).thenReturn("<html>Html</html>");

        doThrow(new RuntimeException("Timeout connessione SMTP")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> strategy.invia(notifica), "L'eccezione deve essere catturata internamente e non propagata.");

        verify(logService).registraEvento(
                eq("Fallimento invio notifica Email"),
                eq(false),
                contains("Timeout connessione SMTP")
        );
    }

    @Test
    void invia_ErroreConCausaAnnidata_FormattaCorrettamenteIlLog() {
        notifica.setMessaggio("Messaggio standard");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/notifica"), any(Context.class))).thenReturn("<html>Html</html>");

        Throwable rootCause = new RuntimeException("Socket chiuso inaspettatamente");
        doThrow(new RuntimeException("Errore invio mail", rootCause)).when(mailSender).send(any(MimeMessage.class));

        strategy.invia(notifica);

        verify(logService).registraEvento(
                eq("Fallimento invio notifica Email"),
                eq(false),
                contains("Causa: Socket chiuso inaspettatamente")
        );
    }
}
