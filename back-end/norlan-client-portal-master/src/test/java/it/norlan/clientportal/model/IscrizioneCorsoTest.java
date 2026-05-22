package it.norlan.clientportal.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
/**
 * Suite di collaudo (Unit Test) per l'entità associativa di dominio (Many-to-Many con payload).
 * Verifica l'incapsulamento della logica di business (Domain-Driven Design) e il corretto mapping
 * delle chiavi primarie composite (Embedded ID) per la persistenza relazionale (JPA/Hibernate).
 */
class IscrizioneCorsoTest {

    private IscrizioneCorso iscrizione;

    @BeforeEach
    void setUp() {
        iscrizione = new IscrizioneCorso();
    }
    // Validazione degli Invarianti di Classe: garantisce che l'entità nasca in uno stato sicuro e deterministico (Fail-Safe), prevenendo falsi positivi critici (es. presenze fittizie)
    @Test
    void costruttore_InizializzaPresenzaAFalse() {
        assertNotNull(iscrizione);
        assertFalse(iscrizione.getPresenzaConfermata());
    }

    // Encapsulation e DDD ("Tell, Don't Ask"): collauda i mutatori semantici di dominio al posto dei banali setter, centralizzando e proteggendo l'evoluzione dello stato interno
    @Test
    void validaPresenza_ImpostaPresenzaATrue() {
        iscrizione.validaPresenza();
        assertTrue(iscrizione.getPresenzaConfermata());
    }

    @Test
    void invalidaPresenza_ImpostaPresenzaAFalse() {
        iscrizione.validaPresenza();
        assertTrue(iscrizione.getPresenzaConfermata());

        iscrizione.invalidaPresenza();
        assertFalse(iscrizione.getPresenzaConfermata());
    }

    // Object-Relational Mapping (ORM): verifica l'integrità strutturale della chiave composita (Composite Key) necessaria per identificare univocamente l'associazione Utente-Corso
    @Test
    void embeddedId_GetterSetter_FunzionanoCorrettamente() {
        IscrizioneCorso.IscrizioneId id = new IscrizioneCorso.IscrizioneId(1, 10);

        iscrizione.setId(id);

        assertNotNull(iscrizione.getId());
        assertEquals(1, iscrizione.getId().getIdUtente());
        assertEquals(10, iscrizione.getId().getIdCorso());
    }

    @Test
    void propertySettersGetters_AssociazioniFunzionanoCorrettamente() {
        Utente utenteMock = mock(Utente.class);
        CorsoFormazione corsoMock = mock(CorsoFormazione.class);
        Documento documentoMock = mock(Documento.class);

        iscrizione.setUtente(utenteMock);
        iscrizione.setCorso(corsoMock);
        iscrizione.setDocumentoAttestato(documentoMock);

        assertEquals(utenteMock, iscrizione.getUtente());
        assertEquals(corsoMock, iscrizione.getCorso());
        assertEquals(documentoMock, iscrizione.getDocumentoAttestato());
    }
}
