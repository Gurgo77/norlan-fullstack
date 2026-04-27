package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.MessaggioDTO;
import it.norlan.clientportal.model.Messaggio;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.MessaggioRepository;
import it.norlan.clientportal.repository.UtenteRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.DipendenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessaggioService {

    @Autowired
    private MessaggioRepository messaggioRepository;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private DipendenteRepository dipendenteRepository;
    @Autowired
    private IscrizioneCorsoRepository iscrizioneCorsoRepository;
    @Autowired
    private NotificaService notificaService;

    @Transactional
    public Messaggio salvaMessaggio(Integer idMittente, Integer idDestinatario, String testo) {
        Utente mittente = utenteRepository.findById(idMittente)
                .orElseThrow(() -> new RuntimeException("Mittente non trovato"));
        Utente destinatario = utenteRepository.findById(idDestinatario)
                .orElseThrow(() -> new RuntimeException("Destinatario non trovato"));

        if (!puoComunicare(mittente, destinatario)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Comunicazione non autorizzata tra " + mittente.getRuolo() + " e " + destinatario.getRuolo()
            );
        }

        Messaggio m = new Messaggio();
        m.setMittente(mittente);
        m.setDestinatario(destinatario);
        m.setTesto(testo);
        m.setTimestampInvio(LocalDateTime.now());

        String testoNotificaInApp = "Hai ricevuto un nuovo messaggio da " + mittente.getEmail();
        String testoNotificaEmail = "" + mittente.getEmail() + "|" + testo;

        notificaService.inviaNotifica(
                destinatario,
                testoNotificaInApp,
                Notifica.Priorita.MEDIA,
                Notifica.CanaleNotifica.IN_APP
        );

        notificaService.inviaNotifica(
                destinatario,
                testoNotificaEmail,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.EMAIL
        );

        return messaggioRepository.save(m);
    }


    private boolean puoComunicare(Utente m, Utente d) {
        // 1. PASS PARTOUT PER L'ADMIN: L'Amministratore può comunicare con CHIUNQUE
        if (m.getRuolo() == Utente.Ruolo.ADMIN || d.getRuolo() == Utente.Ruolo.ADMIN) {
            return true;
        }

        // 2. Comunicazione Azienda <-> Dipendente
        if (m.getRuolo() == Utente.Ruolo.AZIENDA && d.getRuolo() == Utente.Ruolo.DIPENDENTE) {
            return dipendenteRepository.existsByIdUtenteAndAziendaIdUtente(d.getIdUtente(), m.getIdUtente());
        }
        if (m.getRuolo() == Utente.Ruolo.DIPENDENTE && d.getRuolo() == Utente.Ruolo.AZIENDA) {
            return dipendenteRepository.existsByIdUtenteAndAziendaIdUtente(m.getIdUtente(), d.getIdUtente());
        }

        // 3. Comunicazione Docente <-> Dipendente (solo se iscritto al corso)
        if (m.getRuolo() == Utente.Ruolo.DOCENTE && d.getRuolo() == Utente.Ruolo.DIPENDENTE) {
            return iscrizioneCorsoRepository.isDipendenteIscrittoAlCorsoDelDocente(d.getIdUtente(), m.getIdUtente());
        }
        if (m.getRuolo() == Utente.Ruolo.DIPENDENTE && d.getRuolo() == Utente.Ruolo.DOCENTE) {
            return iscrizioneCorsoRepository.isDipendenteIscrittoAlCorsoDelDocente(m.getIdUtente(), d.getIdUtente());
        }

        return false;
    }

    public MessaggioDTO convertToDTO(Messaggio m) {
        MessaggioDTO dto = new MessaggioDTO();
        dto.setIdMessaggio(m.getIdMessaggio());
        dto.setIdMittente(m.getMittente().getIdUtente());
        dto.setNomeMittente(m.getMittente().getEmail());
        dto.setIdDestinatario(m.getDestinatario().getIdUtente());
        dto.setTesto(m.getTesto());
        dto.setTimestampInvio(m.getTimestampInvio());
        dto.setLetto(m.getLetto());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MessaggioDTO> getCronologia(Integer id1, Integer id2) {
        return messaggioRepository.findConversazione(id1, id2).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
