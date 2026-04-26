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
