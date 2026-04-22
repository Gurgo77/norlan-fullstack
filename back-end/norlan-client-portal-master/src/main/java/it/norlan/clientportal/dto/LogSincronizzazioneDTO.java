package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
