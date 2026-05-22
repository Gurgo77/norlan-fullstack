package it.norlan.clientportal.model;

import it.norlan.clientportal.state.documento.DocumentoState;
import it.norlan.clientportal.state.documento.StatoCaricato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il Behavioral Design Pattern "State" (Lato Context).
 * Valida il ciclo di vita dell'entità di dominio (Rich Domain Model) assicurando la corretta
 * inizializzazione e la delega polimorfica delle transizioni di stato per evitare l'anti-pattern degli if-else annidati.
 */
class DocumentoTest {

    private Documento documento;

    @BeforeEach
    void setUp() {
        documento = new Documento();
    }

    // Object Lifecycle Validation: garantisce che l'istanziazione dell'entità produca un dominio in uno stato consistente e valido (Stato di Default) sin dal momento della creazione
    @Test
    void costruttore_InizializzaConStatoCaricatoEDataOggi() {
        assertNotNull(documento.getStato());
        assertTrue(documento.getStato() instanceof StatoCaricato);
        assertNotNull(documento.getDataCaricamento());
        assertEquals(LocalDate.now(), documento.getDataCaricamento());
    }

    // Verifica del meccanismo di Inversion of Control interno al pattern: dimostra (tramite Mocking) che il Context delega l'esecuzione logica all'implementazione concreta dello Stato corrente
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
