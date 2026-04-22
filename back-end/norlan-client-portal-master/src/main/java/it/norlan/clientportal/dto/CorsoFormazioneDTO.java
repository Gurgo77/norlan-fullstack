package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.CorsoFormazione;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    // --- FLATTENING DOCENTE ---
    private Integer idDocente;
    private String emailDocente; // Recuperata dall'oggetto Utente padre del Docente
}
