package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Suite di collaudo (Unit Test) per il nodo transazionale (StatoInAttesaFirma) della FSM documentale.
 * Verifica la gestione degli stati bloccanti (Blocking States), la prevenzione della ridondanza
 * computazionale e la rigorosa applicazione dei vincoli di sequenzialità del workflow.
 */
class StatoInAttesaFirmaTest {

    private StatoInAttesaFirma statoInAttesaFirma;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoInAttesaFirma = new StatoInAttesaFirma();
        documento = new Documento();
    }

    // Blocking State Idempotency: verifica che la macchina a stati finiti inibisca iterazioni cicliche sul medesimo nodo di attesa, prevenendo anomalie di concorrenza o side-effect multipli
    @Test
    void richiediFirma_GiaInAttesa_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoInAttesaFirma.richiediFirma(documento),
                "La reiterazione della richiesta di firma su un documento già in attesa deve sollevare un'eccezione di stato."
        );

        assertEquals("Il documento è già in attesa di firma.", exception.getMessage());
    }

    // Forward State Mutation: collauda lo sblocco del nodo transazionale tramite l'evento di validazione (Approvazione), garantendo la corretta evoluzione del puntatore polimorfico nel Context
    @Test
    void approva_TransizioneAmmessa_ModificaStatoInApprovato() {
        statoInAttesaFirma.approva(documento);

        assertNotNull(documento.getStato(), "Lo stato del documento non deve essere nullo dopo la transizione.");
        assertInstanceOf(StatoApprovato.class, documento.getStato(), "Il documento deve transitare coerentemente verso l'istanza di StatoApprovato.");
    }

    // Sequential Constraint (Guard Clause): assicura che il pattern respinga transizioni premature verso nodi terminali (Archiviazione), blindando l'iter burocratico obbligatorio contro tentativi di elusione
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
