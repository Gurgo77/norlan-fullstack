package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IscrizioneCorsoDTO {

    // --- ID PIATTI (Dalla chiave composta) ---
    private Integer idUtente;
    private Integer idCorso;

    // --- FLATTENING UTENTE ---
    private String emailUtente;

    // --- FLATTENING CORSO ---
    private String titoloCorso;
    private LocalDateTime dataOrarioCorso;

    // --- DATI ISCRIZIONE ---
    private Boolean presenzaConfermata;
    private String pathAttestato;
}
