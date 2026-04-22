package it.norlan.clientportal.state.documento;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DocumentoStateConverter implements AttributeConverter<DocumentoState, String> {

    @Override
    public String convertToDatabaseColumn(DocumentoState state) {
        if (state == null) {
            return null;
        }
        return state.getNomeStato();
    }

    @Override
    public DocumentoState convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "CARICATO" -> new StatoCaricato();
            case "IN_ATTESA_FIRMA" -> new StatoInAttesaFirma();
            case "APPROVATO" -> new StatoApprovato();
            case "ARCHIVIATO" -> new StatoArchiviato();
            default -> throw new IllegalArgumentException("Stato documento sconosciuto nel DB: " + dbData);
        };
    }
}
