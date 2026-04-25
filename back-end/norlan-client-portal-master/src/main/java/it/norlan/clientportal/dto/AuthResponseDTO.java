package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private Integer idUtente;
    private String email;
    private String ruolo;
    private Boolean richiedeCambioPassword;
}
