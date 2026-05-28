package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Provider e Registry centralizzato per il pattern Factory Method.
 * Mappa i ruoli per un recupero dinamico, mantenendo la stessa interfaccia.
 */
@Component
public class UtenteFactoryProvider {

    private final Map<Utente.Ruolo, UtenteFactory> factoryMap = new EnumMap<>(Utente.Ruolo.class);

    @Autowired
    public UtenteFactoryProvider(List<UtenteFactory> factories) {
        for (UtenteFactory factory : factories) {
            factoryMap.put(factory.getRuoloSupportato(), factory);
        }
    }

    public Utente creaUtente(Utente.Ruolo ruolo) {
        if (ruolo == null) {
            throw new IllegalArgumentException("Impossibile istanziare un Utente: il ruolo non può essere nullo.");
        }

        UtenteFactory factory = factoryMap.get(ruolo);

        if (factory == null) {
            throw new UnsupportedOperationException("Strategia di istanziazione non implementata per il ruolo: " + ruolo);
        }

        return factory.creaUtente();
    }
}
