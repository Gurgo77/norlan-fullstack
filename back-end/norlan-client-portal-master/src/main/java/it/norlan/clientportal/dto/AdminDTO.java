package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) per l'entità Amministratore.
 * Utilizzato per trasferire i dati essenziali del profilo al frontend
 * in modo sicuro, senza esporre informazioni sensibili come la password o i token.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDTO {

    private Integer idUtente;
    private String email;
    private Utente.Ruolo ruolo;
}
