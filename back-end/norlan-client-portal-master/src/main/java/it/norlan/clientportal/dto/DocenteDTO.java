package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) per l'entità Docente.
 * Incapsula i dati anagrafici e la specializzazione tecnica dei formatori aziendali
 * che terranno i corsi di sicurezza.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocenteDTO {

    private Integer idUtente;
    private String nome;
    private String cognome;
    private String email;
    private Utente.Ruolo ruolo;
    private String specializzazioneTecnica;
}
