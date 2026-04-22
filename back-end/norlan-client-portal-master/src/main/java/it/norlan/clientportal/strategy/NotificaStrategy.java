package it.norlan.clientportal.strategy;

import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Notifica.CanaleNotifica;

public interface NotificaStrategy {

    void invia(Notifica notifica);
    CanaleNotifica getCanaleSupportato();
}
