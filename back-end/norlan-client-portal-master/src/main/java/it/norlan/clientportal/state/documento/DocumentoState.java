package it.norlan.clientportal.state.documento;

import it.norlan.clientportal.model.Documento;

/**
 * Interfaccia base del Design Pattern "State" (GoF).
 * Definisce il contratto per le transizioni di stato del documento, sfruttando il polimorfismo
 * per incapsulare il comportamento ed eliminare complesse logiche condizionali (if/switch).
 */

public interface DocumentoState {

    void richiediFirma(Documento documento);
    void approva(Documento documento);
    void archivia(Documento documento);

    String getNomeStato();
}
