package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) per il tracciamento delle iscrizioni.
 * Funge da tabella di giunzione tra Utente e Corso, trasportando i dati sullo stato
 * di presenza, l'emissione dell'attestato finale e il flag per il feedback inviato.
 */

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