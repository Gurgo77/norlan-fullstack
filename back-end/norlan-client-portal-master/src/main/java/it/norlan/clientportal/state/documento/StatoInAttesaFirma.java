package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

public class StatoInAttesaFirma implements DocumentoState {
    @Override
    public void richiediFirma(Documento documento) {
        throw new IllegalStateException("Il documento è già in attesa di firma.");
    }

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
