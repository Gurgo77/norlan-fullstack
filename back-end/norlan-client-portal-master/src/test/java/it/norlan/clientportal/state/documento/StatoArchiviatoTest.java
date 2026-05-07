package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatoArchiviatoTest {

    private StatoArchiviato statoArchiviato;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoArchiviato = new StatoArchiviato();
        documento = new Documento();
    }

    @Test
    void richiediFirma_StatoTerminale_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoArchiviato.richiediFirma(documento),
                "Non deve essere possibile richiedere la firma di un documento già archiviato."
        );

        assertEquals("Documento archiviato: operazione non permessa.", exception.getMessage());
    }

    @Test
    void approva_StatoTerminale_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoArchiviato.approva(documento),
                "Non deve essere possibile approvare un documento già archiviato."
        );

        assertEquals("Documento archiviato: operazione non permessa.", exception.getMessage());
    }

    @Test
    void archivia_GiaArchiviato_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoArchiviato.archivia(documento),
                "Tentare di archiviare un documento già archiviato deve lanciare un'eccezione."
        );

        assertEquals("Il documento è già archiviato.", exception.getMessage());
    }

    @Test
    void getNomeStato_IdentificatoreDeterministico_RitornaCostanteCorretta() {
        String nomeStato = statoArchiviato.getNomeStato();

        assertEquals("ARCHIVIATO", nomeStato, "L'identificatore dello stato deve essere esattamente 'ARCHIVIATO'.");
    }
}
