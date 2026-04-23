package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AziendaDTO {

    // Dati ereditati da Utente
    private Integer idUtente;
    private String email;
    private Utente.Ruolo ruolo;

    // Dati specifici di Azienda
    private String ragioneSociale;
    private String partitaIva;

    // Possiamo aggiungere campi extra che servono a Nicolò (es. per la UI)
    private String etichettaDisplay;

    // ... altri campi esistenti

    private String sedeLegale;
    private String pec;
    private String telefono;
    private String cellulare;
    private String referenteAziendale;
}
