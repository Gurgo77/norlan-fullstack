package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Suite di collaudo (Unit Test) per il nodo terminale (Sink State) della FSM documentale.
 * Verifica l'enforcement dell'immutabilità del workflow, garantendo che un'entità
 * giunta a fine ciclo di vita (Archiviazione) non possa subire ulteriori transizioni di stato.
 */
class StatoArchiviatoTest {

    private StatoArchiviato statoArchiviato;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoArchiviato = new StatoArchiviato();
        documento = new Documento();
    }

    // Terminal State Enforcement (Immutabilità): assicura che il pattern inibisca categoricamente qualsiasi tentativo di regressione o mutazione, "congelando" definitivamente il ciclo di vita del documento
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

    // Idempotency & Sink Node Protection: collauda il blocco difensivo contro transizioni cicliche ridondanti sullo stesso nodo, garantendo la stabilità computazionale dell'entità archiviata
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
