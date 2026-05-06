package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificaContextTest {

    @Mock
    private NotificaStrategy emailStrategy;

    @Mock
    private NotificaStrategy inAppStrategy;

    private NotificaContext notificaContext;

    @BeforeEach
    void setUp() {
        when(emailStrategy.getCanaleSupportato()).thenReturn(Notifica.CanaleNotifica.EMAIL);
        when(inAppStrategy.getCanaleSupportato()).thenReturn(Notifica.CanaleNotifica.IN_APP);

        notificaContext = new NotificaContext(List.of(emailStrategy, inAppStrategy));
    }

    @Test
    void eseguiStrategia_CanaleSupportato_ChiamaMetodoInviaCorretto() {
        Notifica notifica = new Notifica();
        notifica.setCanale(Notifica.CanaleNotifica.EMAIL);

        notificaContext.eseguiStrategia(notifica);

        verify(emailStrategy, times(1)).invia(notifica);
        verify(inAppStrategy, never()).invia(any());
    }

    @Test
    void eseguiStrategia_AltroCanaleSupportato_ChiamaMetodoInviaCorretto() {
        Notifica notifica = new Notifica();
        notifica.setCanale(Notifica.CanaleNotifica.IN_APP);

        notificaContext.eseguiStrategia(notifica);

        verify(inAppStrategy, times(1)).invia(notifica);
        verify(emailStrategy, never()).invia(any());
    }

    @Test
    void eseguiStrategia_CanaleNonSupportato_LanciaUnsupportedOperationException() {
        NotificaContext contestoParziale = new NotificaContext(List.of(emailStrategy));

        Notifica notifica = new Notifica();
        notifica.setCanale(Notifica.CanaleNotifica.IN_APP);

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> contestoParziale.eseguiStrategia(notifica)
        );

        assertEquals("Nessuna strategia trovata per il canale: IN_APP", exception.getMessage());
    }

    @Test
    void eseguiStrategia_CanaleNullo_LanciaUnsupportedOperationException() {
        Notifica notifica = new Notifica();
        notifica.setCanale(null);

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> notificaContext.eseguiStrategia(notifica)
        );

        assertEquals("Nessuna strategia trovata per il canale: null", exception.getMessage());
    }
}
