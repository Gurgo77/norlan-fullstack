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
    private Integer idCorso;
    private String titoloCorso;
    private String titoloDocumento;
    private String percorsoFile;
    private LocalDateTime dataCaricamento;
}
