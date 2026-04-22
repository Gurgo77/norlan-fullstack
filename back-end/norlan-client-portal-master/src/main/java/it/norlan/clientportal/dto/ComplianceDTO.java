package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.LivelloRischio;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComplianceDTO {
    private LivelloRischio statoGlobale;
    private long documentiScaduti;
    private long documentiInScadenza;
    private String messaggioSuggerimento;
}
