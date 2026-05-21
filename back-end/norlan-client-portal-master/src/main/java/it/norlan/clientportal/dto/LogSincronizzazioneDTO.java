package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) per i log di sistema e di sicurezza.
 * Trasporta le informazioni sugli eventi tracciati (esito, note tecniche, timestamp)
 * per popolare i cruscotti di auditing dell'Amministratore.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogSincronizzazioneDTO {

    private Integer idLog;
    private String descrizioneEvento;
    private LocalDateTime dataEvento;
    private Boolean esitoPositivo;
    private String noteTecniche;
}
