package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) per l'entità Dipendente (Lavoratore).
 * Mappa le informazioni anagrafiche, il codice fiscale e i collegamenti all'azienda
 * di appartenenza, schermando eventuali dati sensibili non necessari alla vista.
 */

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
