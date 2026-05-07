package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatoApprovatoTest {

    private StatoApprovato statoApprovato;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoApprovato = new StatoApprovato();
        documento = new Documento();
    }

    @Test
    void richiediFirma_StatoIncompatibile_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoApprovato.richiediFirma(documento),
                "La richiesta di firma su un documento già approvato deve lanciare un'eccezione di stato illegale."
        );

        assertEquals("Documento già approvato, impossibile richiedere firma.", exception.getMessage());
    }

    @Test
    void approva_StatoGiaApprovato_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoApprovato.approva(documento),
                "L'approvazione di un documento che si trova già nello stato APPROVATO deve lanciare un'eccezione."
        );

        assertEquals("Il documento è già stato approvato.", exception.getMessage());
    }

    @Test
    void archivia_TransizioneAmmessa_ModificaStatoInArchiviato() {
        statoApprovato.archivia(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoArchiviato.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoArchiviato.");
    }

    @Test
    void getNomeStato_IdentificatoreDeterministico_RitornaCostanteCorretta() {
        String nomeStato = statoApprovato.getNomeStato();

        assertEquals("APPROVATO", nomeStato, "L'identificatore dello stato deve corrispondere esattamente alla costante definita per le mappature ORM.");
    }
}
