package it.norlan.clientportal.dto;

import lombok.Data;
import java.time.LocalDateTime;

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
