package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.AssegnazioneDPI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) per la gestione dei Dispositivi di Protezione Individuale.
 * Mappa i dettagli di consegna e le date di scadenza/revisione del DPI assegnato al dipendente.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssegnazioneDPIDTO {

    private Integer idAssegnazione;
    private Integer idDipendente;
    private AssegnazioneDPI.TipoDPI tipo;
    private LocalDate dataConsegna;
    private LocalDate dataScadenzaRevisione;
    private boolean daRevisionare;
    private String nomeDpi;
}
