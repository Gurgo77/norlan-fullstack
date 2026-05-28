package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Utente;

/**
 * Interfaccia base del Design Pattern "Factory Method" (GoF).
 * Definisce il contratto architetturale per la creazione polimorfica
 * degli utenti, delegando l'istanziazione concreta alle sottoclassi.
 */
public interface UtenteFactory {

    Utente creaUtente();
    Utente.Ruolo getRuoloSupportato();
}
