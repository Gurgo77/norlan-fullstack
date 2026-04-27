package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.CorsoFormazioneDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.model.MaterialeDidattico;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.MaterialeDidatticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CorsoFormazioneService {

    @Autowired
    private CorsoFormazioneRepository corsoRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    // Aggiunto il repository per gestire i materiali didattici collegati
    @Autowired
    private MaterialeDidatticoRepository materialeRepository;

    @Autowired
    private NotificaService notificaService;

    @Transactional(readOnly = true)
    public List<CorsoFormazione> findAll() {
        return corsoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CorsoFormazione> findById(Integer id) {
        return corsoRepository.findById(id);
    }

    @Transactional
    public void eliminaCorso(Integer id) {
        // 1. Eliminiamo prima tutti i materiali didattici associati al corso
        List<MaterialeDidattico> materiali = materialeRepository.findByCorsoIdCorso(id);
        if (!materiali.isEmpty()) {
            materialeRepository.deleteAll(materiali);
        }

        // 2. Eliminiamo tutte le iscrizioni associate al corso
        List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByCorsoIdCorso(id);
        if (!iscrizioni.isEmpty()) {
            iscrizioneRepository.deleteAll(iscrizioni);
        }

        // 3. Ora possiamo eliminare il corso in modo totalmente sicuro per il DB
        corsoRepository.deleteById(id);
    }

    @Transactional
    public CorsoFormazione salvaCorso(CorsoFormazione corso) {
        if (corso.getDataOrario() != null && corso.getDataOrario().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Non puoi programmare un corso nel passato.");
        }

        return corsoRepository.save(corso);
    }

    @Transactional
    public void aggiornaStato(Integer idCorso, CorsoFormazione.StatoCorso nuovoStato) {
        corsoRepository.findById(idCorso).ifPresent(corso -> {
            corso.setStato(nuovoStato);
            CorsoFormazione salvato = corsoRepository.save(corso);

            if (salvato.getDocente() != null) {
                notificaService.inviaNotifica(
                        salvato.getDocente(),
                        "Avviso: Lo stato del corso '" + salvato.getTitolo() + "' è stato modificato in: " + nuovoStato,
                        Notifica.Priorita.ALTA,
                        Notifica.CanaleNotifica.IN_APP
                );
            }
        });
    }

    @Transactional
    public void concludiCorso(Integer idCorso) {
        CorsoFormazione corso = corsoRepository.findById(idCorso).orElseThrow();
        corso.setStato(CorsoFormazione.StatoCorso.CONCLUSO);
        corsoRepository.save(corso);

        List<IscrizioneCorso> partecipanti =  iscrizioneRepository.findByCorsoAndPresenzaConfermataTrue(corso);

        for(IscrizioneCorso partecipante : partecipanti) {
            String messaggio = "Il corso '" + corso.getTitolo() + "' si è concluso. Ti invitiamo a lasciare un feedback qualitativo sulla piattaforma.";
            notificaService.inviaNotifica(partecipante.getUtente(), messaggio, Notifica.Priorita.MEDIA, Notifica.CanaleNotifica.IN_APP);
        }
    }

    @Transactional(readOnly = true)
    public List<CorsoFormazione> trovaCorsiPerStato(CorsoFormazione.StatoCorso stato) {
        return corsoRepository.findByStato(stato);
    }

    public CorsoFormazioneDTO convertToDTO(CorsoFormazione corso) {
        CorsoFormazioneDTO dto = new CorsoFormazioneDTO();

        dto.setIdCorso(corso.getIdCorso());
        dto.setTitolo(corso.getTitolo());
        dto.setDataOrario(corso.getDataOrario());
        dto.setLuogoFisico(corso.getLuogoFisico());
        dto.setStato(corso.getStato());

        if (corso.getDocente() != null) {
            dto.setIdDocente(corso.getDocente().getIdUtente());
            dto.setEmailDocente(corso.getDocente().getEmail());
        }

        return dto;
    }
}
