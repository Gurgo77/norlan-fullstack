package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialeDidatticoDTO {

    private Integer idMateriale;

    // --- FLATTENING DEL CORSO ---
    private Integer idCorso;
    private String titoloCorso;

    // --- DATI DEL DOCUMENTO ---
    private String titoloDocumento;
    private String percorsoFile;
    private LocalDateTime dataCaricamento;
}
