package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente.Ruolo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) "polimorfico" per le richieste di Autenticazione e Onboarding.
 * Incapsula sia le credenziali di login (email, password), sia l'intero set di campi anagrafici
 * necessari per la registrazione dinamica di nuove Aziende, Docenti o Dipendenti.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    private String email;
    private String password;
    private Ruolo ruolo;
    private String ragioneSociale;
    private String partitaIva;
    private String sedeLegale;
    private String pec;
    private String telefono;
    private String cellulare;
    private String referenteAziendale;
    private String nome;
    private String cognome;
    private String codiceFiscale;
    private Integer idAzienda;
    private String specializzazione;
}
