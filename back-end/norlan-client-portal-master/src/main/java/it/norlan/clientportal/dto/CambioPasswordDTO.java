package it.norlan.clientportal.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) per la richiesta di aggiornamento delle credenziali.
 * Trasporta la vecchia e la nuova password durante la procedura di reset volontario o
 * durante l'aggiornamento obbligatorio richiesto al primo accesso.
 */

@Data
public class CambioPasswordDTO {
    private String vecchiaPassword;
    private String nuovaPassword;
}
