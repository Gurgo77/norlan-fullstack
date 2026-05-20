package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

/**
 * Implementazione concreta del Design Pattern "State" (Stato: APPROVATO).
 * Definisce il comportamento del documento post-validazione, inibendo operazioni pregresse
 * (es. nuova richiesta di firma) e consentendo unicamente l'avanzamento verso lo stoccaggio (Archiviazione).
 */

public class StatoApprovato implements DocumentoState {
    @Override
    public void richiediFirma(Documento documento) {
        throw new IllegalStateException("Documento già approvato, impossibile richiedere firma.");
    }

    // Blocca proattivamente le transizioni di stato illegali lanciando eccezioni a runtime (Approccio Fail-Fast)
    @Override
    public void approva(Documento documento) {
        throw new IllegalStateException("Il documento è già stato approvato.");
    }

    // Applica l'unica transizione di stato valida, mutando dinamicamente il comportamento dell'entità verso lo step successivo (ARCHIVIATO)
    @Override
    public void archivia(Documento documento) {
        documento.setStato(new StatoArchiviato());
    }

    @Override
    public String getNomeStato() {
        return "APPROVATO";
    }
}
