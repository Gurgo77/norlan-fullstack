package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DipendenteDTO {

    private Integer idUtente;
    private String nome;
    private String cognome;
    private String email;
    private Utente.Ruolo ruolo;
    private String password;
    private String codiceFiscale;
    private Integer idAzienda;
    private String ragioneSocialeAzienda;
}
