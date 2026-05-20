package it.norlan.clientportal.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) per la messaggistica (Chat).
 * Utilizzato sia sui canali WebSocket (tempo reale) sia tramite REST (recupero storico),
 * trasporta il mittente, il destinatario, il contenuto e lo stato di lettura.
 */

@Data
public class MessaggioDTO {

    private Integer idMessaggio;
    private Integer idMittente;
    private String nomeMittente;
    private Integer idDestinatario;
    private String testo;
    private LocalDateTime timestampInvio;
    private Boolean letto;
}
