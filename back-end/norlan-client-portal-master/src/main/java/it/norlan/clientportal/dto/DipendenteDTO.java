package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DipendenteDTO {

    // Dati ereditati da Utente
    private Integer idUtente;
    private String nome;
    private String cognome;
    private String email;
    private Utente.Ruolo ruolo;

    // Dati specifici di Dipendente
    private String codiceFiscale;

    // --- FLATTENING DELL'AZIENDA ---
    // Non mandiamo l'intero oggetto Azienda, ma solo i riferimenti utili
    private Integer idAzienda;
    private String ragioneSocialeAzienda;
}
