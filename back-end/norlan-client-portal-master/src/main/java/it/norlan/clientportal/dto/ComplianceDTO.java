package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.LivelloRischio;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object (DTO) per il report di Conformità Normativa (Compliance).
 * Aggrega e trasporta le metriche sullo stato di rischio aziendale, conteggiando
 * i documenti scaduti o in scadenza per alimentare i cruscotti (dashboard) del frontend.
 */

@Data
@AllArgsConstructor
public class ComplianceDTO {
    private LivelloRischio statoGlobale;
    private long documentiScaduti;
    private long documentiInScadenza;
    private String messaggioSuggerimento;
}
