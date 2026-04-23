package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Utente.Ruolo;
import lombok.Data;

@Data
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
