package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Data Transfer Object (DTO) per la ricezione dei moduli di valutazione (Feedback).
 * Implementa le annotazioni di Jakarta Validation (@NotNull, @Min, @Max) per garantire
 * l'integrità formale dei voti e dei commenti prima del salvataggio a database.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatsDTO {
    private Integer idCorso;
    private Double mediaDocenza;
    private Double mediaContenuti;
    private Long totaleFeedback;
    private List<String> commenti;
}
