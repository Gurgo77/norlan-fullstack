package it.norlan.clientportal.dto;

import it.norlan.clientportal.model.Documento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoDTO {

    private Integer idDocumento;

    // --- FLATTENING AZIENDA ---
    private Integer idAzienda;
    private String ragioneSocialeAzienda;

    // --- ENUM E DATI ---
    private Documento.ModuloServizio modulo;
    private Documento.TipoDocumento tipologia;
    private String stato;

    private String filePath;
    private LocalDate dataCaricamento;
    private LocalDate dataScadenza;

    // --- CAMPI CALCOLATI PER IL FRONTEND ---
    private boolean scaduto;
}
