package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

/**
 * Implementazione concreta del Design Pattern "State" (Stato Iniziale: CARICATO).
 * Rappresenta l'entry-point della Macchina a Stati Finiti (FSM), smistando il workflow
 * verso le fasi di validazione e impedendo "scorciatoie" illegali per la business logic.
 */

public class StatoCaricato implements DocumentoState {

    // Innesca la transizione di stato regolare verso la fase di validazione formale (passaggio del testimone nell'automa)
    @Override
    public void richiediFirma(Documento documento) {
        documento.setStato(new StatoInAttesaFirma());
    }

    @Override
    public void approva(Documento documento) {
        documento.setStato(new StatoApprovato());
    }

    // Applica un vincolo di sicurezza (Guard Clause) per impedire l'archiviazione diretta, forzando il rispetto del flusso approvativo
    @Override
    public void archivia(Documento documento) {
        throw new IllegalStateException("Impossibile archiviare un documento appena caricato. Deve prima essere approvato.");
    }

    @Override
    public String getNomeStato() {
        return "CARICATO";
    }
}
