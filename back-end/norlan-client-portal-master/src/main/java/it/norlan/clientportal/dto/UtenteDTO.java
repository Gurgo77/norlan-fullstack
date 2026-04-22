package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtenteDTO {

    private Integer idUtente;
    private String email;
    private Utente.Ruolo ruolo;

    // Campo opzionale utile per il frontend
    private String tipoUtente;
}
