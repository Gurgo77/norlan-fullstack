package it.norlan.clientportal.state.documento;

import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatoCaricatoTest {

    private StatoCaricato statoCaricato;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoCaricato = new StatoCaricato();
        documento = new Documento();
    }

    @Test
    void richiediFirma_TransizioneAmmessa_ModificaStatoInAttesaFirma() {
        statoCaricato.richiediFirma(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoInAttesaFirma.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoInAttesaFirma.");
    }

    @Test
    void approva_TransizioneAmmessa_ModificaStatoInApprovato() {
        statoCaricato.approva(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoApprovato.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoApprovato.");
    }

    @Test
    void archivia_TransizioneNonAmmessa_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoCaricato.archivia(documento),
                "Il tentativo di archiviare un documento appena caricato salta passaggi logici e deve sollevare un'eccezione."
        );

        assertEquals("Impossibile archiviare un documento appena caricato. Deve prima essere approvato.", exception.getMessage());
    }

    @Test
    void getNomeStato_IdentificatoreDeterministico_RitornaCostanteCorretta() {
        String nomeStato = statoCaricato.getNomeStato();

        assertEquals("CARICATO", nomeStato, "L'identificatore dello stato deve corrispondere esattamente alla costante definita 'CARICATO'.");
    }
}
