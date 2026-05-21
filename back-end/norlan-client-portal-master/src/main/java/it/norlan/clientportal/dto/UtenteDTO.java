package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) generico per l'entità di base Utente.
 * Fornisce una rappresentazione minimale e sicura dell'account (email e ruolo),
 * utilizzabile nei contesti in cui non sono richiesti i dettagli anagrafici completi.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtenteDTO {

    private Integer idUtente;
    private String email;
    private Utente.Ruolo ruolo;
    private String tipoUtente;
}
