package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

public class StatoApprovato implements DocumentoState {
    @Override
    public void richiediFirma(Documento documento) {
        throw new IllegalStateException("Documento già approvato, impossibile richiedere firma.");
    }

    @Override
    public void approva(Documento documento) {
        throw new IllegalStateException("Il documento è già stato approvato.");
    }

    @Override
    public void archivia(Documento documento) {
        documento.setStato(new StatoArchiviato());
    }

    @Override
    public String getNomeStato() {
        return "APPROVATO";
    }
}
