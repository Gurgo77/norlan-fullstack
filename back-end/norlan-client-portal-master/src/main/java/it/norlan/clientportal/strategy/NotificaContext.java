package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificaContext {

    private final Map<CanaleNotifica, NotificaStrategy> strategyMap = new EnumMap<>(CanaleNotifica.class);

    @Autowired
    public NotificaContext(List<NotificaStrategy> strategies) {
        for (NotificaStrategy strategy : strategies) {
            strategyMap.put(strategy.getCanaleSupportato(), strategy);
        }
    }

    public void eseguiStrategia(Notifica notifica) {
        NotificaStrategy strategy = strategyMap.get(notifica.getCanale());

        if (strategy == null) {
            throw new UnsupportedOperationException("Nessuna strategia trovata per il canale: " + notifica.getCanale());
        }

        strategy.invia(notifica);
    }
}
