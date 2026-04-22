package it.norlan.clientportal.state.documento;

import it.norlan.clientportal.model.Documento;

public interface DocumentoState {

    void richiediFirma(Documento documento);
    void approva(Documento documento);
    void archivia(Documento documento);

    String getNomeStato();
}
