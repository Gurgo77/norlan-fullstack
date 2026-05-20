package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.IscrizioneCorsoDTO;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.model.IscrizioneCorso.IscrizioneId;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.UtenteRepository;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.repository.FeedbackRepository; // <-- AGGIUNTO IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Livello di servizio (Business Logic) per la gestione delle iscrizioni didattiche (Join Table).
 * Sfrutta chiavi primarie composite (IscrizioneId) e implementa un sofisticato sistema
 * di notifica multi-attore (fan-out) per sincronizzare dipendenti, docenti e aziende.
 */

@Service
public class IscrizioneCorsoService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private CorsoFormazioneRepository corsoRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private FeedbackRepository feedbackRepository; // <-- AGGIUNTA DEPENDENCY INJECTION

    @Transactional(readOnly = true)
    public List<IscrizioneCorso> findAll() {
        return iscrizioneRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IscrizioneCorso> trovaIscrizioniByCorso(Integer idCorso) {
        return iscrizioneRepository.findByCorsoIdCorso(idCorso);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Integer idUtente, Integer idCorso) {
        return iscrizioneRepository.existsById(new IscrizioneId(idUtente, idCorso));
    }

    @Transactional
    public void eliminaIscrizione(Integer idUtente, Integer idCorso) {
        iscrizioneRepository.deleteById(new IscrizioneId(idUtente, idCorso));
    }

    // Previene iscrizioni duplicate e innesca un routing di notifiche multi-canale mirato ai tre attori coinvolti (Studente, Docente, Azienda)
    @Transactional
    public IscrizioneCorso iscriviUtente(Integer idUtente, Integer idCorso) {
        IscrizioneId id = new IscrizioneId(idUtente, idCorso);

        if (iscrizioneRepository.existsById(id)) {
            throw new IllegalStateException("L'utente è già iscritto a questo corso.");
        }

        Utente utente = utenteRepository.findById(idUtente)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        CorsoFormazione corso = corsoRepository.findById(idCorso)
                .orElseThrow(() -> new IllegalArgumentException("Corso non trovato"));

        IscrizioneCorso iscrizione = new IscrizioneCorso();
        iscrizione.setId(id);
        iscrizione.setUtente(utente);
        iscrizione.setCorso(corso);
        iscrizione.setPresenzaConfermata(false);

        notificaService.inviaNotifica(
                utente,
                "Sei stato iscritto al corso: " + corso.getTitolo(),
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.IN_APP
        );

        notificaService.inviaNotifica(
                corso.getDocente(),
                "Un nuovo dipendente si è iscritto al tuo corso.",
                Notifica.Priorita.BASSA,
                Notifica.CanaleNotifica.IN_APP
        );

        String dataCorsoStr = corso.getDataOrario() != null ? corso.getDataOrario().toString() : "Data da definire";
        String luogoCorsoStr = corso.getLuogoFisico() != null ? corso.getLuogoFisico() : "Sede da definire";

        String messaggioEmailDipendente = "Sei stato ufficialmente iscritto al corso di formazione: <b>" + corso.getTitolo() + "</b>.<br><br>"
                + "Dettagli logistici del corso:<br>"
                + "<ul>"
                + "<li><b>Data e Ora:</b> " + dataCorsoStr + "</li>"
                + "<li><b>Sede:</b> " + luogoCorsoStr + "</li>"
                + "</ul><br>"
                + "Accedi al portale NorLan per scaricare l'eventuale materiale didattico.";

        notificaService.inviaNotifica(
                utente,
                messaggioEmailDipendente,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.EMAIL
        );

        String messaggioEmailDocente =
                "Il registro del corso <b>" +
                        corso.getTitolo() +
                        "</b> è stato aggiornato.<br>" +
                        "Il dipendente (" + utente.getEmail() +
                        ") è stato aggiunto alla lista dei partecipanti in aula.";

        notificaService.inviaNotifica(
                corso.getDocente(),
                messaggioEmailDocente,
                Notifica.Priorita.MEDIA,
                Notifica.CanaleNotifica.EMAIL
        );

        if (utente instanceof Dipendente) {
            Dipendente dip = (Dipendente) utente;
            if (dip.getAzienda() != null) {
                String msgAzienda = "Il tuo dipendente " + dip.getNome() + " " + dip.getCognome() + " è stato iscritto al corso: " + corso.getTitolo() + ".";
                notificaService.inviaNotifica(
                        dip.getAzienda(),
                        msgAzienda,
                        Notifica.Priorita.BASSA,
                        Notifica.CanaleNotifica.IN_APP
                );
            }
        }
        return iscrizioneRepository.save(iscrizione);
    }

    // Valida la presenza in aula recuperando il record di iscrizione esatto tramite la chiave composita (Lavoratore-Corso)    @Transactional
    public void validaPresenzaLavoratore(Integer idCorso, Integer idLavoratore) {
        IscrizioneId id = new IscrizioneId(idLavoratore, idCorso);

        IscrizioneCorso iscrizione = iscrizioneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Iscrizione non trovata per il lavoratore e il corso specificati."));

        iscrizione.validaPresenza();
        iscrizioneRepository.save(iscrizione);
    }

    @Transactional(readOnly = true)
    public String getPathAttestato(Integer idLavoratore, Integer idCorso) {
        IscrizioneCorso iscrizione = iscrizioneRepository.findById(new IscrizioneId(idLavoratore, idCorso))
                .orElseThrow(() -> new IllegalArgumentException("Iscrizione non trovata"));

        return iscrizione.getDocumentoAttestato() != null ? iscrizione.getDocumentoAttestato().getFilePath() : null;
    }

    @Transactional(readOnly = true)
    public List<IscrizioneCorso> trovaIscrizioniUtente(Integer idUtente) {
        return iscrizioneRepository.findByUtenteIdUtente(idUtente);
    }

    // Mappa l'entità nel DTO arricchendolo dinamicamente a runtime: interroga il modulo Feedback per iniettare lo stato della recensione
    public IscrizioneCorsoDTO convertToDTO(IscrizioneCorso iscrizione) {
        IscrizioneCorsoDTO dto = new IscrizioneCorsoDTO();

        if (iscrizione.getId() != null) {
            dto.setIdUtente(iscrizione.getId().getIdUtente());
            dto.setIdCorso(iscrizione.getId().getIdCorso());
            boolean hasFeedback = feedbackRepository
                    .findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(dto.getIdUtente(), dto.getIdCorso())
                    .isPresent();

            dto.setFeedbackInviato(hasFeedback);
        }

        if (iscrizione.getUtente() != null) {
            dto.setEmailUtente(iscrizione.getUtente().getEmail());
        }

        if (iscrizione.getCorso() != null) {
            dto.setTitoloCorso(iscrizione.getCorso().getTitolo());
            dto.setDataOrarioCorso(iscrizione.getCorso().getDataOrario());
            if (iscrizione.getCorso().getStato() != null) {
                dto.setStatoCorso(iscrizione.getCorso().getStato().name());
            }
        }

        dto.setPresenzaConfermata(iscrizione.getPresenzaConfermata());

        if (iscrizione.getDocumentoAttestato() != null) {
            dto.setIdDocumento(iscrizione.getDocumentoAttestato().getIdDocumento());
        }

        return dto;
    }
}