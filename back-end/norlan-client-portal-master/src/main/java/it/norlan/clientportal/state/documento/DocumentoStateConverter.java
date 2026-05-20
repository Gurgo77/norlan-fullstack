package it.norlan.clientportal.state.documento;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute Converter (Mapping ORM avanzato).
 * Funge da ponte bidirezionale tra il paradigma a oggetti e quello relazionale: serializza
 * gli oggetti polimorfici "State" in formato testuale (String) per il database e li ricostruisce a runtime.
 */

@Converter(autoApply = true)
public class DocumentoStateConverter implements AttributeConverter<DocumentoState, String> {

    // Serializza l'istanza concreta dello stato estraendone l'identificativo testuale per la persistenza su disco
    @Override
    public String convertToDatabaseColumn(DocumentoState state) {
        if (state == null) {
            return null;
        }
        return state.getNomeStato();
    }

    // Idratazione ORM (Rehydration): agisce come un Factory Method per instanziare la corretta classe di stato concreta a partire dal dato grezzo del database
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
