package it.norlan.clientportal.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * Data Transfer Object (DTO) per la ricezione dei moduli di valutazione (Feedback).
 * Implementa le annotazioni di Jakarta Validation (@NotNull, @Min, @Max) per garantire
 * l'integrità formale dei voti e dei commenti prima del salvataggio a database.
 */

@Data
public class FeedbackDTO {

    @NotNull(message = "L'ID utente è obbligatorio")
    private Integer idUtente;

    @NotNull(message = "L'ID corso è obbligatorio")
    private Integer idCorso;

    @Min(1) @Max(5)
    @NotNull(message = "Il rating della docenza è obbligatorio")
    private Integer ratingDocenza;

    @Min(1) @Max(5)
    @NotNull(message = "Il rating dei contenuti è obbligatorio")
    private Integer ratingContenuti;

    @Size(max = 1000, message = "Il commento non può superare i 1000 caratteri")
    private String commento;
}
