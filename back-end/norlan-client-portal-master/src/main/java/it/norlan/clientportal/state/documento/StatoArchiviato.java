package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

public class StatoArchiviato implements DocumentoState {
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
