package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Suite di collaudo (Unit Test) per il nodo iniziale (Entry Node) della FSM documentale.
 * Verifica il bootstrapping del ciclo di vita (State Pattern), la gestione delle biforcazioni
 * del workflow (Branching) e la protezione contro l'elusione dei passaggi logici obbligatori (Anti-Bypass).
 */
class StatoCaricatoTest {

    private StatoCaricato statoCaricato;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoCaricato = new StatoCaricato();
        documento = new Documento();
    }

    // FSM State Forwarding: collauda la transizione standard verso il nodo successivo, assicurando la corretta mutazione a runtime del puntatore polimorfico all'interno del Context (Documento)
    @Test
    void richiediFirma_TransizioneAmmessa_ModificaStatoInAttesaFirma() {
        statoCaricato.richiediFirma(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoInAttesaFirma.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoInAttesaFirma.");
    }

    // Workflow Fast-Tracking (Biforcazione Logica): verifica la flessibilità architetturale nell'ammettere percorsi alternativi validi (approvazione diretta), dimostrando che la FSM non è vincolata a una sequenzialità rigida
    @Test
    void approva_TransizioneAmmessa_ModificaStatoInApprovato() {
        statoCaricato.approva(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoApprovato.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoApprovato.");
    }

    // Sequential Integrity (Anti-Bypass Guard): garantisce che l'entità rigetti categoricamente i tentativi di elusione del workflow (es. archiviazione prematura), blindando gli invarianti burocratici di dominio
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
