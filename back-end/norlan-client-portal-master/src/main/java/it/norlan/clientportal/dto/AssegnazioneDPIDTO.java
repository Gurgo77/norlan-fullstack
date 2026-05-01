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
    private Integer idDipendente;
    private AssegnazioneDPI.TipoDPI tipo;
    private LocalDate dataConsegna;
    private LocalDate dataScadenzaRevisione;
    private boolean daRevisionare;
    private String nomeDpi;
}
