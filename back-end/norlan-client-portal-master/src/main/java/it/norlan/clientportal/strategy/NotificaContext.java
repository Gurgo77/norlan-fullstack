package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Classe "Context" del Design Pattern "Strategy".
 * Agisce come un router dinamico (Dispatcher) che incapsula la mappa degli algoritmi disponibili
 * e risolve a runtime la corretta implementazione di invio in base al canale richiesto.
 */

@Component
public class NotificaContext {

    private final Map<CanaleNotifica, NotificaStrategy> strategyMap = new EnumMap<>(CanaleNotifica.class);

    // Sfrutta l'Inversion of Control (IoC) per auto-scoprire le strategie implementate a runtime, indicizzandole in un EnumMap per lookup ad altissime prestazioni O(1)
    @Autowired
    public NotificaContext(List<NotificaStrategy> strategies) {
        for (NotificaStrategy strategy : strategies) {
            strategyMap.put(strategy.getCanaleSupportato(), strategy);
        }
    }

    // Risolve l'algoritmo corretto tramite la chiave di routing (Canale) ed esegue l'invio polimorfico, adottando un approccio Fail-Fast in caso di canale non supportato
    public void eseguiStrategia(Notifica notifica) {
        NotificaStrategy strategy = strategyMap.get(notifica.getCanale());

        if (strategy == null) {
            throw new UnsupportedOperationException("Nessuna strategia trovata per il canale: " + notifica.getCanale());
        }

        strategy.invia(notifica);
    }
}
