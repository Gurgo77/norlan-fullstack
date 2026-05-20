package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) per le richieste di rinnovo documentale.
 * Trasferisce i dati per tracciare il workflow e gli stati di approvazione
 * delle pratiche di rinnovo richieste dalle aziende o generate dal sistema.
 */

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
