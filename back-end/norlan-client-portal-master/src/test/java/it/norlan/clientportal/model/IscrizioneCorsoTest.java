package it.norlan.clientportal.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class IscrizioneCorsoTest {

    private IscrizioneCorso iscrizione;

    @BeforeEach
    void setUp() {
        iscrizione = new IscrizioneCorso();
    }

    @Test
    void costruttore_InizializzaPresenzaAFalse() {
        assertNotNull(iscrizione);
        assertFalse(iscrizione.getPresenzaConfermata());
    }

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
