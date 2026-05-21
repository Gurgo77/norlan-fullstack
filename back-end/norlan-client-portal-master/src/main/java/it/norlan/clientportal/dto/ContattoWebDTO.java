package it.norlan.clientportal.dto;
import lombok.Data;

/**
 * Data Transfer Object (DTO) per i moduli di contatto pubblici.
 * Raccoglie i dati inviati dai visitatori non autenticati tramite la landing page,
 * includendo il flag di validazione per l'accettazione esplicita della privacy policy.
 */

@Data
public class ContattoWebDTO {
    private String nome;
    private String cognome;
    private String email;
    private String messaggio;
    private boolean privacyAccepted;
}
