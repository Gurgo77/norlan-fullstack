package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.MessaggioDTO;
import it.norlan.clientportal.model.Messaggio;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import it.norlan.clientportal.service.MessaggioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.AccessDeniedException;
import java.security.Principal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di comunicazione Real-Time (WebSocket/STOMP).
 * Verifica il routing asincrono dei messaggi tramite il Message Broker e collauda
 * i meccanismi di gestione centralizzata delle eccezioni (Error Handling) sul tunnel bidirezionale.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessaggioService messaggioService;

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
    }

    // Valida il pattern architetturale Point-to-Point: verifica la persistenza del payload e il corretto instradamento (Routing Push) del messaggio al client destinatario tramite il Broker
    @Test
    void sendMessage_SalvaMessaggioEInviaAlDestinatario() {
        MessaggioDTO payload = new MessaggioDTO();
        payload.setIdMittente(1);
        payload.setIdDestinatario(2);
        payload.setTesto("Ciao!");

        Messaggio messaggioSalvato = new Messaggio();
        MessaggioDTO dtoRisposta = new MessaggioDTO();
        dtoRisposta.setTesto("Ciao!");

        when(messaggioService.salvaMessaggio(1, 2, "Ciao!")).thenReturn(messaggioSalvato);
        when(messaggioService.convertToDTO(messaggioSalvato)).thenReturn(dtoRisposta);

        chatController.sendMessage(payload);

        verify(messagingTemplate).convertAndSendToUser(
                eq("2"),
                eq("/queue/messages"),
                eq(dtoRisposta)
        );
    }

    // Testa la Fault Tolerance sul socket: assicura che le violazioni di sicurezza vengano intercettate, notificate privatamente all'utente (Dead Letter Queue) e tracciate nell'Audit Log
    @Test
    void handleException_ConPrincipal_InviaErroreELogga() {
        AccessDeniedException exception = new AccessDeniedException("Accesso negato");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@norlan.it");

        chatController.handleException(exception, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user@norlan.it"),
                eq("/queue/errors"),
                eq("Accesso negato")
        );

        verify(logService).registraEvento(
                eq("Tentativo di accesso non autorizzato"),
                eq(false),
                eq("L'utente user@norlan.it ha tentato un'azione bloccata via WebSocket. Motivo: Accesso negato")
        );
    }

    // Verifica la robustezza dell'Error Handler (Fail-Safe): garantisce che l'elaborazione dell'eccezione abortisca in modo sicuro se il frame STOMP è sprovvisto di contesto di sicurezza
    @Test
    void handleException_SenzaPrincipal_NonInviaErrore() {
        AccessDeniedException exception = new AccessDeniedException("Accesso negato");

        chatController.handleException(exception, null);

        verifyNoInteractions(messagingTemplate);
        verifyNoInteractions(logService);
    }
}
