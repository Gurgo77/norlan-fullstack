package it.norlan.clientportal.state.documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * Suite di collaudo (Unit Test) per il layer di traduzione ORM (JPA Attribute Converter).
 * Verifica la risoluzione dell'Object-Relational Impedance Mismatch, garantendo la corretta
 * serializzazione/deserializzazione del Behavioral Pattern (State) verso colonne scalari del database.
 */
class DocumentoStateConverterTest {

    private DocumentoStateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DocumentoStateConverter();
    }

    // ORM Boundary Null-Safety: garantisce che la pipeline di serializzazione verso il database sia resiliente ai valori nulli, prevenendo NullPointerException durante le fasi di persistenza (Flush)
    @Test
    void convertToDatabaseColumn_StatoNull_RitornaNull() {
        String result = converter.convertToDatabaseColumn(null);

        assertNull(result, "La conversione di uno stato null deve restituire null per  DB.");
    }

    @Test
    void convertToDatabaseColumn_StatoValido_RitornaNomeStato() {
        DocumentoState mockState = mock(DocumentoState.class);
        when(mockState.getNomeStato()).thenReturn("IN_ATTESA_FIRMA");

        String result = converter.convertToDatabaseColumn(mockState);

        assertEquals("IN_ATTESA_FIRMA", result, "La conversione verso il DB deve estrarre esattamente il nome dello stato.");
    }

    @Test
    void convertToEntityAttribute_StringaNull_RitornaNull() {
        DocumentoState result = converter.convertToEntityAttribute(null);

        assertNull(result, "La lettura di un valore null dal DB deve restituire uno stato null.");
    }

    // Object Rehydration (Deserialization Routing): collauda il meccanismo di reidratazione del Dominio, assicurando che un dato scalare (Stringa DB) venga istanziato dinamicamente nella corretta classe concreta dello State Pattern
    @Test
    void convertToEntityAttribute_Caricato_RitornaIstanzaStatoCaricato() {
        DocumentoState result = converter.convertToEntityAttribute("CARICATO");

        assertInstanceOf(StatoCaricato.class, result, "La stringa 'CARICATO' deve essere mappata alla classe StatoCaricato.");
    }

    @Test
    void convertToEntityAttribute_InAttesaFirma_RitornaIstanzaStatoInAttesaFirma() {
        DocumentoState result = converter.convertToEntityAttribute("IN_ATTESA_FIRMA");

        assertInstanceOf(StatoInAttesaFirma.class, result, "La stringa 'IN_ATTESA_FIRMA' deve essere mappata alla classe StatoInAttesaFirma.");
    }

    @Test
    void convertToEntityAttribute_Approvato_RitornaIstanzaStatoApprovato() {
        DocumentoState result = converter.convertToEntityAttribute("APPROVATO");

        assertInstanceOf(StatoApprovato.class, result, "La stringa 'APPROVATO' deve essere mappata alla classe StatoApprovato.");
    }

    @Test
    void convertToEntityAttribute_Archiviato_RitornaIstanzaStatoArchiviato() {
        DocumentoState result = converter.convertToEntityAttribute("ARCHIVIATO");

        assertInstanceOf(StatoArchiviato.class, result, "La stringa 'ARCHIVIATO' deve essere mappata alla classe StatoArchiviato.");
    }

    // Data Corruption Guard Clause: difende l'integrità del Dominio intercettando "Dirty Reads" o anomalie del database (es. stringhe manomesse), bloccando il caricamento di entità in stati inconsistenti
    @Test
    void convertToEntityAttribute_ValoreSconosciuto_LanciaEccezione() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("STATO_ANOMALO")
        );

        assertEquals("Stato documento sconosciuto nel DB: STATO_ANOMALO", exception.getMessage());
    }
}
