package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.CorsoFormazione;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) per l'entità Corso di Formazione.
 * Trasporta le informazioni di base del corso, inclusi i dettagli logistici (data, luogo, capacità massima)
 * e i riferimenti al docente assegnato.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorsoFormazioneDTO {

    private Integer idCorso;
    private String titolo;
    private LocalDateTime dataOrario;
    private String luogoFisico;
    private Integer capacitaMassima;
    private CorsoFormazione.StatoCorso stato;
    private Integer idDocente;
    private String emailDocente;
}
