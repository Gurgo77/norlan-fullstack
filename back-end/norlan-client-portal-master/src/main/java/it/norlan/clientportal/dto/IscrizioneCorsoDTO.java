package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IscrizioneCorsoDTO {

    private Integer idUtente;
    private Integer idCorso;
    private String emailUtente;
    private String titoloCorso;
    private LocalDateTime dataOrarioCorso;
    private Boolean presenzaConfermata;
    private Integer idDocumento;
    private String statoCorso;
    private Boolean feedbackInviato;
}