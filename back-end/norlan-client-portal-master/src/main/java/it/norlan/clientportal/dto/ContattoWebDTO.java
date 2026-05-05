package it.norlan.clientportal.dto;
import lombok.Data;

@Data
public class ContattoWebDTO {
    private String nome;
    private String cognome;
    private String email;
    private String messaggio;
    private boolean privacyAccepted;
}
