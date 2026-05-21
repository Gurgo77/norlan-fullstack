package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.MaterialeDidatticoDTO;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.model.MaterialeDidattico;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.MaterialeDidatticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Livello di servizio (Business Logic) per la gestione del Materiale Didattico.
 * Centralizza l'archiviazione logica delle risorse formative e implementa un sistema di notifica
 * di tipo "fan-out" per avvisare in tempo reale (In-App ed Email) tutti gli studenti iscritti.
 */

@Service
public class MaterialeDidatticoService {

    @Autowired
    private MaterialeDidatticoRepository repository;

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Transactional(readOnly = true)
    public List<MaterialeDidattico> trovaPerCorso(Integer idCorso) {
        return repository.findByCorsoIdCorso(idCorso);
    }

    @Transactional(readOnly = true)
    public Optional<MaterialeDidattico> findById(Integer id) {
        return repository.findById(id);
    }

    // Valida l'integrità del percorso file, persiste il record e innesca un ciclo di notifiche (Broadcasting) rivolto all'intera platea degli iscritti
    @Transactional
    public MaterialeDidattico salvaMateriale(MaterialeDidattico materiale) {
        if (materiale.getDataCaricamento() == null) {
            materiale.setDataCaricamento(LocalDateTime.now());
        }
        if (materiale.getPercorsoFile() == null || materiale.getPercorsoFile().isBlank()) {
            throw new IllegalArgumentException("Il percorso del file è obbligatorio per il materiale didattico.");
        }

        MaterialeDidattico salvato = repository.save(materiale);

        if (salvato.getCorso() != null && salvato.getCorso().getDocente() != null) {
            notificaService.inviaNotifica(
                    salvato.getCorso().getDocente(),
                    "È stato caricato con successo il materiale didattico: " + salvato.getTitoloDocumento(),
                    Notifica.Priorita.BASSA,
                    Notifica.CanaleNotifica.IN_APP
            );
        }

        List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByCorsoIdCorso(salvato.getCorso().getIdCorso());

        String msgDiscenteInApp = "Nuovo materiale caricato per il corso: " + salvato.getCorso().getTitolo();

        String msgDiscenteEmail = "Nuovo materiale didattico disponibile per il corso <b>" + salvato.getCorso().getTitolo() + "</b>.<br>"
                + "Il docente ha reso disponibile il documento: <i>" + salvato.getTitoloDocumento() + "</i>.<br>"
                + "Ti invitiamo ad accedere alla tua area riservata per scaricarlo e prepararti adeguatamente alla lezione.";

        for (IscrizioneCorso isc : iscrizioni) {
            notificaService.inviaNotifica(
                    isc.getUtente(),
                    msgDiscenteInApp,
                    Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.IN_APP
            );

            notificaService.inviaNotifica(
                    isc.getUtente(),
                    msgDiscenteEmail,
                    Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.EMAIL
            );
        }

        return salvato;
    }

    @Transactional
    public void eliminaMateriale(Integer idMateriale) {
        repository.deleteById(idMateriale);
    }

    // Trasforma l'entità complessa nel rispettivo DTO, estraendo solo i riferimenti essenziali del corso per ottimizzare il payload HTTP verso il client
    public MaterialeDidatticoDTO convertToDTO(MaterialeDidattico materiale) {
        MaterialeDidatticoDTO dto = new MaterialeDidatticoDTO();

        dto.setIdMateriale(materiale.getIdMateriale());

        if (materiale.getCorso() != null) {
            dto.setIdCorso(materiale.getCorso().getIdCorso());
            dto.setTitoloCorso(materiale.getCorso().getTitolo());
        }

        dto.setTitoloDocumento(materiale.getTitoloDocumento());
        dto.setPercorsoFile(materiale.getPercorsoFile());
        dto.setDataCaricamento(materiale.getDataCaricamento());

        return dto;
    }
}
