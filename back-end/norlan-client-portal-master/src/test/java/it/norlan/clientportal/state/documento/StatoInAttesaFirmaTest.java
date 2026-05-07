package it.norlan.clientportal.state.documento;

import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatoInAttesaFirmaTest {

    private StatoInAttesaFirma statoInAttesaFirma;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoInAttesaFirma = new StatoInAttesaFirma();
        documento = new Documento();
    }

    @Test
    void richiediFirma_GiaInAttesa_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoInAttesaFirma.richiediFirma(documento),
                "La reiterazione della richiesta di firma su un documento già in attesa deve sollevare un'eccezione di stato."
        );

        assertEquals("Il documento è già in attesa di firma.", exception.getMessage());
    }

    @Test
    void approva_TransizioneAmmessa_ModificaStatoInApprovato() {
        statoInAttesaFirma.approva(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoApprovato.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoApprovato.");
    }

    @Test
    void archivia_TransizioneNonAmmessa_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoInAttesaFirma.archivia(documento),
                "Tentare di archiviare un documento in attesa di firma costituisce una violazione del flusso logico e deve fallire."
        );

        assertEquals("Impossibile archiviare un documento non ancora approvato.", exception.getMessage());
    }

    @Test
    void getNomeStato_IdentificatoreDeterministico_RitornaCostanteCorretta() {
        String nomeStato = statoInAttesaFirma.getNomeStato();

        assertEquals("IN_ATTESA_FIRMA", nomeStato, "L'identificatore dello stato deve corrispondere alla costante strutturale 'IN_ATTESA_FIRMA'.");
    }
}
