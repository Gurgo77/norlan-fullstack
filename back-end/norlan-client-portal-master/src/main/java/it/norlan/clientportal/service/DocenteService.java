package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DocenteDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.repository.DocenteRepository;
import it.norlan.clientportal.repository.MessaggioRepository;
import it.norlan.clientportal.repository.NotificaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Livello di servizio (Business Logic) per la gestione dell'anagrafica Docenti (Formatori).
 */
@Service
public class DocenteService {

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogSincronizzazioneService logService;

    @Autowired
    private CorsoFormazioneRepository corsoRepository;

    @Autowired
    private CorsoFormazioneService corsoService; // Per l'eliminazione sicura dei corsi

    @Autowired
    private NotificaRepository notificaRepository;

    @Autowired
    private MessaggioRepository messaggioRepository;

    @Transactional(readOnly = true)
    public List<Docente> findAll() {
        return docenteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Docente> findById(Integer id) {
        return docenteRepository.findById(id);
    }

    @Transactional
    public Docente salvaDocente(Docente docente) {
        if (docente.getSpecializzazioneTecnica() == null || docente.getSpecializzazioneTecnica().isBlank()) {
            throw new IllegalArgumentException("La specializzazione del docente è obbligatoria.");
        }
        if (docente.getPasswordHash() != null) {
            docente.setPasswordHash(passwordEncoder.encode(docente.getPasswordHash()));
        }
        return docenteRepository.save(docente);
    }

    @Transactional
    public void eliminaDocente(Integer id) {
        Docente docente = docenteRepository.findById(id).orElseThrow();
        String nomeCompleto = docente.getNome() + " " + docente.getCognome();

        List<CorsoFormazione> corsi = corsoRepository.findByDocenteIdUtente(id);
        for (CorsoFormazione corso : corsi) {
            corsoService.eliminaCorso(corso.getIdCorso());
        }

        notificaRepository.deleteAll(notificaRepository.findByDestinatarioIdUtenteOrderByDataInvioDesc(id));

        messaggioRepository.deleteAll(messaggioRepository.findAll().stream()
                .filter(m -> m.getMittente().getIdUtente().equals(id) || m.getDestinatario().getIdUtente().equals(id))
                .toList());

        docenteRepository.deleteById(id);

        logService.registraEvento(
                "Eliminazione anagrafica: DOCENTE",
                true,
                "Cancellato docente ID: " + id + " (" + nomeCompleto + ") e tutte le dipendenze (Corsi, Notifiche, Chat)"
        );
    }

    public DocenteDTO convertToDTO(Docente docente) {
        DocenteDTO dto = new DocenteDTO();
        dto.setIdUtente(docente.getIdUtente());
        dto.setNome(docente.getNome());
        dto.setCognome(docente.getCognome());
        dto.setEmail(docente.getEmail());
        dto.setRuolo(docente.getRuolo());
        dto.setSpecializzazioneTecnica(docente.getSpecializzazioneTecnica());
        return dto;
    }
}