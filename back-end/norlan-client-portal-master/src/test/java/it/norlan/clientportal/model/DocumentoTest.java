package it.norlan.clientportal.model;

import it.norlan.clientportal.state.documento.DocumentoState;
import it.norlan.clientportal.state.documento.StatoCaricato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoTest {

    private Documento documento;

    @BeforeEach
    void setUp() {
        documento = new Documento();
    }

    @Test
    void costruttore_InizializzaConStatoCaricatoEDataOggi() {
        assertNotNull(documento.getStato());
        assertTrue(documento.getStato() instanceof StatoCaricato);
        assertNotNull(documento.getDataCaricamento());
        assertEquals(LocalDate.now(), documento.getDataCaricamento());
    }

    @Test
    void richiediFirma_DelegaAlloStatoCorrente() {
        DocumentoState mockStato = mock(DocumentoState.class);
        documento.setStato(mockStato);

        documento.richiediFirma();

        verify(mockStato).richiediFirma(documento);
    }

    @Test
    void approva_DelegaAlloStatoCorrente() {
        DocumentoState mockStato = mock(DocumentoState.class);
        documento.setStato(mockStato);

        documento.approva();

        verify(mockStato).approva(documento);
    }

    @Test
    void archivia_DelegaAlloStatoCorrente() {
        DocumentoState mockStato = mock(DocumentoState.class);
        documento.setStato(mockStato);

        documento.archivia();

        verify(mockStato).archivia(documento);
    }

    @Test
    void propertySettersGetters_FunzionanoCorrettamente() {
        Azienda azienda = new Azienda();
        azienda.setRagioneSociale("NorLan Corp");

        LocalDate scadenza = LocalDate.now().plusDays(30);

        documento.setIdDocumento(1);
        documento.setAzienda(azienda);
        documento.setModulo(Documento.ModuloServizio.SICUREZZA);
        documento.setTipologia(Documento.TipoDocumento.DVR);
        documento.setFilePath("/docs/dvr.pdf");
        documento.setDataScadenza(scadenza);

        assertEquals(1, documento.getIdDocumento());
        assertEquals("NorLan Corp", documento.getAzienda().getRagioneSociale());
        assertEquals(Documento.ModuloServizio.SICUREZZA, documento.getModulo());
        assertEquals(Documento.TipoDocumento.DVR, documento.getTipologia());
        assertEquals("/docs/dvr.pdf", documento.getFilePath());
        assertEquals(scadenza, documento.getDataScadenza());
    }
}
