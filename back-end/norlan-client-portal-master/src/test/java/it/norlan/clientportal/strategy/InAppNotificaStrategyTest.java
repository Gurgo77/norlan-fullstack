package it.norlan.clientportal.strategy;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.NotificaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InAppNotificaStrategyTest {

    @Mock
    private NotificaRepository notificaRepository;

    @InjectMocks
    private InAppNotificaStrategy strategy;

    private Notifica notifica;
    private Utente destinatario;

    @BeforeEach
    void setUp() {
        destinatario = new Utente() {};
        destinatario.setEmail("utente@test.it");

        notifica = new Notifica();
        notifica.setDestinatario(destinatario);
        notifica.setMessaggio("Notifica di test in-app");
    }

    @Test
    void invia_SalvaNotificaNelDatabase() {
        strategy.invia(notifica);

        verify(notificaRepository).save(notifica);
    }

    @Test
    void getCanaleSupportato_RitornaInApp() {
        Notifica.CanaleNotifica canale = strategy.getCanaleSupportato();

        assertEquals(Notifica.CanaleNotifica.IN_APP, canale, "La strategia deve dichiarare presenza canale via IN_APP.");
    }
}
