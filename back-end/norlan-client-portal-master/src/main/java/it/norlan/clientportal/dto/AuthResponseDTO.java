package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object (DTO) per la risposta di avvenuta autenticazione.
 * Restituisce al client il token JWT valido per la sessione e i metadati fondamentali
 * dell'utente (ruolo, ID e flag per il cambio password obbligatorio).
 */

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private Integer idUtente;
    private String email;
    private String ruolo;
    private Boolean richiedeCambioPassword;
}
