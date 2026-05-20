package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) per l'entità Azienda.
 * Incapsula i dati anagrafici e fiscali dell'azienda cliente (Partita IVA, PEC, referente),
 * fornendo al frontend le informazioni strutturate per la visualizzazione e la modifica.
 */

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
