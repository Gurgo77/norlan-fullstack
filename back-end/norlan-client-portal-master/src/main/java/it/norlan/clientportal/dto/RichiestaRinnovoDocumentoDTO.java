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

    // --- FLATTENING DOCUMENTO ---
    private Integer idDocumento;
    private String tipologiaDocumento; // es. "DVR", "HACCP"
    private String ragioneSocialeAzienda; // Per l'Admin è utile sapere di chi è il documento

    // --- DATI RICHIESTA ---
    private LocalDateTime dataRichiesta;
    private RichiestaRinnovoDocumento.StatoRinnovo stato;
}
