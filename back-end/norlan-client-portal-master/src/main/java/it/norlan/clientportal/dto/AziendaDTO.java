package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AziendaDTO {

    private Integer idUtente;
    private String email;
    private Utente.Ruolo ruolo;
    private String ragioneSociale;
    private String partitaIva;
    private String etichettaDisplay;
    private boolean hasDipendenti;
    private String sedeLegale;
    private String pec;
    private String telefono;
    private String cellulare;
    private String referenteAziendale;
}
