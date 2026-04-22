package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDTO {

    // Dati ereditati dalla gerarchia Utente
    private Integer idUtente;
    private String email;
    private Utente.Ruolo ruolo;

    // Al momento non ci sono campi extra,
    // ma la struttura è pronta per future espansioni.
}
