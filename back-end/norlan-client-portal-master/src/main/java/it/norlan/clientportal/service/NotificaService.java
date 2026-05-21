package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.NotificaDTO;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.NotificaRepository;
import it.norlan.clientportal.strategy.NotificaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Livello di servizio (Business Logic) per l'orchestrazione delle comunicazioni.
 * Sfrutta l'elaborazione asincrona (Multithreading via @Async) per non bloccare i thread HTTP
 * e adotta il Design Pattern "Strategy" per disaccoppiare la logica di invio in base al canale (Email, In-App).
 */

@Service
public class NotificaService {

    @Autowired
    private NotificaContext notificaContext;

    @Autowired
    private NotificaRepository notificaRepository;

    @Transactional(readOnly = true)
    public Optional<Notifica> findById(Integer id) {
        return notificaRepository.findById(id);
    }

    @Transactional
    public void eliminaNotifica(Integer id) {
        notificaRepository.deleteById(id);
    }

    public List<Notifica> getNotificheNonLette(Integer idUtente) {
        return notificaRepository.findByDestinatarioIdUtenteAndLettaFalseOrderByDataInvioDesc(idUtente);
    }

    // Sposta il task di notifica su un thread pool dedicato in background, delegando l'effettivo delivery al "Strategy Pattern" per massimizzare le performance
    @Async("notificheExecutor")
    @Transactional
    public void inviaNotifica(Utente destinatario, String messaggio, Notifica.Priorita priorita, Notifica.CanaleNotifica canale) {

        Notifica notifica = new Notifica();
        notifica.setDestinatario(destinatario);
        notifica.setMessaggio(messaggio);
        notifica.setPriorita(priorita);
        notifica.setCanale(canale);
        notifica.setDataInvio(LocalDateTime.now());
        notifica.setLetta(false);

        notificaContext.eseguiStrategia(notifica);
    }

    // Aggiorna lo stato di lettura del messaggio gestendo la concorrenza tramite Optimistic Locking, prevenendo scritture sporche
    @Transactional
    public void segnaComeLetta(Integer idNotifica) {
        notificaRepository.findById(idNotifica).ifPresent(n -> {
            n.setLetta(true);
            notificaRepository.save(n);
        });
    }

    @Transactional(readOnly = true)
    public List<Notifica> getNotificheUtente(Integer idUtente) {
        return notificaRepository.findByDestinatarioIdUtenteOrderByDataInvioDesc(idUtente);
    }

    @Transactional(readOnly = true)
    public long contaNonLette(Integer idUtente) {
        return notificaRepository.countByDestinatarioIdUtenteAndLettaFalse(idUtente);
    }

    // Mappa l'entità nel DTO serializzabile per il frontend, incapsulando i dati critici e prevenendo l'esposizione diretta dell'oggetto di dominio
    public NotificaDTO convertToDTO(Notifica notifica) {
        NotificaDTO dto = new NotificaDTO();

        dto.setIdNotifica(notifica.getIdNotifica());
        if (notifica.getDestinatario() != null) {
            dto.setIdDestinatario(notifica.getDestinatario().getIdUtente());
            dto.setEmailDestinatario(notifica.getDestinatario().getEmail());
        }

        dto.setMessaggio(notifica.getMessaggio());
        dto.setLetta(notifica.getLetta());
        dto.setPriorita(notifica.getPriorita());
        dto.setDataInvio(notifica.getDataInvio());

        return dto;
    }
}
