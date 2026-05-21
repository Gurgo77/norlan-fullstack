package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) per i materiali didattici.
 * Mappa i riferimenti fisici e logici delle dispense allegate ai corsi di formazione,
 * permettendo al frontend di generare i link per il download.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialeDidatticoDTO {

    private Integer idMateriale;
    private Integer idCorso;
    private String titoloCorso;
    private String titoloDocumento;
    private String percorsoFile;
    private LocalDateTime dataCaricamento;
}
