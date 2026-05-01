package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Notifica;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificaDTO {

    private Integer idNotifica;
    private Integer idDestinatario;
    private String emailDestinatario;
    private String messaggio;
    private Boolean letta;
    private Notifica.Priorita priorita;
    private LocalDateTime dataInvio;
}
