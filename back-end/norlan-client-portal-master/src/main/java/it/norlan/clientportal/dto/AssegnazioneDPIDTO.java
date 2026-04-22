package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.AssegnazioneDPI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssegnazioneDPIDTO {

    private Integer idAssegnazione;

    // --- FLATTENING DIPENDENTE ---
    private Integer idDipendente;

    // --- DATI DPI ---
    private AssegnazioneDPI.TipoDPI tipo;
    private LocalDate dataConsegna;
    private LocalDate dataScadenzaRevisione;

    // --- LOGICA DI SICUREZZA ---
    private boolean daRevisionare;
}
