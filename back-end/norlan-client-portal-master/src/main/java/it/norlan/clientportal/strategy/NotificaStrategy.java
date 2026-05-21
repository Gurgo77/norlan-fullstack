package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;

/**
 * Interfaccia base del Design Pattern "Strategy" (GoF).
 * Definisce il contratto architetturale che tutte le strategie di comunicazione
 * devono rispettare, garantendo la totale intercambiabilità degli algoritmi a runtime (Open/Closed Principle).
 */

public interface NotificaStrategy {

    // Esegue l'algoritmo specifico di inoltro del messaggio (es. SMTP, salvataggio a DB) nascondendone la complessità al client
    void invia(Notifica notifica);
    // Restituisce la chiave di routing associata all'algoritmo per consentire al Context l'indicizzazione automatica (Discovery)
    CanaleNotifica getCanaleSupportato();
}
