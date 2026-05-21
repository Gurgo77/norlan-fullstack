package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

/**
 * Implementazione concreta del Design Pattern "State" (Stato Terminale: ARCHIVIATO).
 * Rappresenta la fine del ciclo di vita del documento (Sink State), imponendo l'immutabilità
 * assoluta dell'entità e bloccando per ragioni di audit qualsiasi ulteriore transizione di stato.
 */

public class StatoArchiviato implements DocumentoState {

    // Sigilla il record storico respingendo a runtime qualsiasi tentativo di alterazione (garanzia di immutabilità)
    @Override
    public void richiediFirma(Documento documento) {
        throw new IllegalStateException("Documento archiviato: operazione non permessa.");
    }

    @Override
    public void approva(Documento documento) {
        throw new IllegalStateException("Documento archiviato: operazione non permessa.");
    }

    @Override
    public void archivia(Documento documento) {
        throw new IllegalStateException("Il documento è già archiviato.");
    }

    @Override
    public String getNomeStato() {
        return "ARCHIVIATO";
    }
}
