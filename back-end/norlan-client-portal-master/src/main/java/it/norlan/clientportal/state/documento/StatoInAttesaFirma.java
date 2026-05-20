package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

/**
 * Implementazione concreta del Design Pattern "State" (Stato Intermedio: IN_ATTESA_FIRMA).
 * Rappresenta la fase di attesa attiva del workflow, in cui l'automa richiede un input esterno
 * (la validazione/firma) per sbloccare la pratica, proteggendo il dominio da transizioni premature.
 */

public class StatoInAttesaFirma implements DocumentoState {

    // Previene richieste duplicate o cicli infiniti rigettando l'operazione ridondante tramite il principio Fail-Fast
    @Override
    public void richiediFirma(Documento documento) {
        throw new IllegalStateException("Il documento è già in attesa di firma.");
    }

    // Recepisce l'input di validazione e sblocca il workflow documentale, mutando lo stato interno dell'entità verso lo step successivo (APPROVATO)
    @Override
    public void approva(Documento documento) {
        documento.setStato(new StatoApprovato());
    }

    @Override
    public void archivia(Documento documento) {
        throw new IllegalStateException("Impossibile archiviare un documento non ancora approvato.");
    }

    @Override
    public String getNomeStato() {
        return "IN_ATTESA_FIRMA";
    }
}
