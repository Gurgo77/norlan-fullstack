package it.norlan.clientportal.state.documento;
import it.norlan.clientportal.model.Documento;

public class StatoCaricato implements DocumentoState {
    @Override
    public void richiediFirma(Documento documento) {
        documento.setStato(new StatoInAttesaFirma());
    }

    @Override
    public void approva(Documento documento) {
        documento.setStato(new StatoApprovato());
    }

    @Override
    public void archivia(Documento documento) {
        throw new IllegalStateException("Impossibile archiviare un documento appena caricato. Deve prima essere approvato.");
    }

    @Override
    public String getNomeStato() {
        return "CARICATO";
    }
}
