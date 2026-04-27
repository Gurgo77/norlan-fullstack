package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.IscrizioneCorsoDTO;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.model.IscrizioneCorso.IscrizioneId;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.UtenteRepository;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.CorsoFormazione;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

        return iscrizioneRepository.save(iscrizione);
    }

    @Transactional
    public void validaPresenzaLavoratore(Integer idCorso, Integer idLavoratore) {
        IscrizioneId id = new IscrizioneId(idLavoratore, idCorso);

        IscrizioneCorso iscrizione = iscrizioneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Iscrizione non trovata per il lavoratore e il corso specificati."));

        iscrizione.validaPresenza();
        iscrizioneRepository.save(iscrizione);
    }

    @Transactional
    public void rilasciaCertificato(Integer idCorso, Integer idLavoratore, String pathFileCertificato) {
        IscrizioneId id = new IscrizioneId(idLavoratore, idCorso);

        IscrizioneCorso iscrizione = iscrizioneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Iscrizione non trovata."));

        iscrizione.sbloccaCertificato(pathFileCertificato);

        iscrizioneRepository.save(iscrizione);

        notificaService.inviaNotifica(
                iscrizione.getUtente(),
                "Il tuo certificato per il corso " + iscrizione.getCorso().getTitolo() + " è ora disponibile per il download.",
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.IN_APP
        );
    }

    @Transactional(readOnly = true)
    public String getPathAttestato(Integer idLavoratore, Integer idCorso) {
        return iscrizioneRepository.findById(new IscrizioneId(idLavoratore, idCorso))
                .map(IscrizioneCorso::getPathAttestato)
                .orElseThrow(() -> new IllegalArgumentException("Iscrizione non trovata"));
    }


    @Transactional(readOnly = true)
    public List<IscrizioneCorso> trovaIscrizioniUtente(Integer idUtente) {
        return iscrizioneRepository.findByUtenteIdUtente(idUtente);
    }

    public IscrizioneCorsoDTO convertToDTO(IscrizioneCorso iscrizione) {
        IscrizioneCorsoDTO dto = new IscrizioneCorsoDTO();

        if (iscrizione.getId() != null) {
            dto.setIdUtente(iscrizione.getId().getIdUtente());
            dto.setIdCorso(iscrizione.getId().getIdCorso());
        }

        if (iscrizione.getUtente() != null) {
            dto.setEmailUtente(iscrizione.getUtente().getEmail());
        }

        if (iscrizione.getCorso() != null) {
            dto.setTitoloCorso(iscrizione.getCorso().getTitolo());
            dto.setDataOrarioCorso(iscrizione.getCorso().getDataOrario());
        }

        dto.setPresenzaConfermata(iscrizione.getPresenzaConfermata());
        dto.setPathAttestato(iscrizione.getPathAttestato());

        return dto;
    }
}
