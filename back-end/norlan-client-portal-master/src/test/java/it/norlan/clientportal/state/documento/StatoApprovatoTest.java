package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Suite di collaudo (Unit Test) per il nodo intermedio (StatoApprovato) della FSM documentale.
 * Verifica l'implementazione del Behavioral Pattern (State), garantendo il rigoroso blocco
 * delle transizioni di workflow illegali e validando l'evoluzione coerente verso l'archiviazione.
 */
class StatoApprovatoTest {

    private StatoApprovato statoApprovato;
    private Documento documento;

    @BeforeEach
    void setUp() {
        statoApprovato = new StatoApprovato();
        documento = new Documento();
    }

    // FSM Constraint Enforcement (Anti-Regression): verifica che il nodo di stato rigetti tentativi di regressione logica (da Approvato a In Attesa), blindando la direzionalità e l'integrità del workflow
    @Test
    void richiediFirma_StatoIncompatibile_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoApprovato.richiediFirma(documento),
                "La richiesta di firma su un documento già approvato deve lanciare un'eccezione di stato illegale."
        );

        assertEquals("Documento già approvato, impossibile richiedere firma.", exception.getMessage());
    }

    // Idempotency & Redundancy Prevention: collauda il blocco difensivo che inibisce la ri-sottomissione dello stesso comando sul medesimo stato, prevenendo mutazioni ridondanti e side-effect indesiderati
    @Test
    void approva_StatoGiaApprovato_LanciaIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> statoApprovato.approva(documento),
                "L'approvazione di un documento che si trova già nello stato APPROVATO deve lanciare un'eccezione."
        );

        assertEquals("Il documento è già stato approvato.", exception.getMessage());
    }

    // Context Mutation (State Transition): assicura che l'invocazione di un evento logicamente consentito muti a runtime il puntatore polimorfico del Context (Documento) verso la successiva classe concreta (StatoArchiviato)
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
