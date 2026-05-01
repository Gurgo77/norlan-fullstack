package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.MessaggioDTO;
import it.norlan.clientportal.model.Messaggio;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import it.norlan.clientportal.service.MessaggioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.nio.file.AccessDeniedException;
import java.security.Principal;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessaggioService messaggioService;

    @Autowired
    private LogSincronizzazioneService logService;

    @MessageMapping("/chat.send")
    public void sendMessage(MessaggioDTO payload) {
        Messaggio salvato = messaggioService.salvaMessaggio(
                payload.getIdMittente(),
                payload.getIdDestinatario(),
                payload.getTesto()
        );

        MessaggioDTO dto = messaggioService.convertToDTO(salvato);

        messagingTemplate.convertAndSendToUser(
                payload.getIdDestinatario().toString(),
                "/queue/messages",
                dto
        );
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    public void handleException(AccessDeniedException e, Principal principal) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    e.getMessage()
            );

            logService.registraEvento(
                    "Tentativo di accesso non autorizzato",
                    false,
                    "L'utente " + principal.getName() + " ha tentato un'azione bloccata via WebSocket. Motivo: " + e.getMessage()
            );
        }
    }
}
