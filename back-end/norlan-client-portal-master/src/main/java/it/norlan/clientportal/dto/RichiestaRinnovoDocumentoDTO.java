package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RichiestaRinnovoDocumentoDTO {

    private Integer idRichiesta;
    private Integer idDocumento;
    private String tipologiaDocumento;
    private String ragioneSocialeAzienda;
    private LocalDateTime dataRichiesta;
    private RichiestaRinnovoDocumento.StatoRinnovo stato;
}
