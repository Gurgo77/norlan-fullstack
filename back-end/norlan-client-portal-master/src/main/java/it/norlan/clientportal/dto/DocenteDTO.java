package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocenteDTO {

    // Dati ereditati dalla gerarchia Utente
    private Integer idUtente;
    private String nome;
    private String cognome;
    private String email;
    private Utente.Ruolo ruolo;

    // Dati specifici del Docente
    private String specializzazioneTecnica;
}
