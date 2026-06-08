package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.CorsoFormazioneDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.Feedback;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.model.MaterialeDidattico;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.repository.FeedbackRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.MaterialeDidatticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Livello di servizio (Business Logic) per l'orchestrazione dei Corsi di Formazione.
 * Gestisce l'intero ciclo di vita didattico implementando una rigorosa Macchina a Stati Finiti (FSM)
 * che impedisce transizioni illegali e coordina log, iscrizioni e comunicazioni asincrone con i partecipanti.
 */
@Service
public class CorsoFormazioneService {

    @Autowired
    private CorsoFormazioneRepository corsoRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Autowired
    private MaterialeDidatticoRepository materialeRepository;

    @Autowired
    private FeedbackRepository feedbackRepository; // Aggiunto per l'eliminazione a cascata dei feedback

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private LogSincronizzazioneService logService;

    @Autowired
    private AdminService adminService;

    @Transactional(readOnly = true)
    public List<CorsoFormazione> findAll() {
        return corsoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CorsoFormazione> findById(Integer id) {
        return corsoRepository.findById(id);
    }

    /**
     * Elimina il corso eseguendo una pulizia a cascata per prevenire errori 409 (Foreign Key Constraint).
     * Ordine di eliminazione: Materiali -> Feedback -> Iscrizioni -> Corso.
     */
    @Transactional
    public void eliminaCorso(Integer id) {
        List<MaterialeDidattico> materiali = materialeRepository.findByCorsoIdCorso(id);
        if (!materiali.isEmpty()) {
            materialeRepository.deleteAll(materiali);
        }

        List<Feedback> feedbackDelCorso = feedbackRepository.findByIscrizione_Id_IdCorso(id);
        if (!feedbackDelCorso.isEmpty()) {
            feedbackRepository.deleteAll(feedbackDelCorso);
        }

        List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByCorsoIdCorso(id);
        if (!iscrizioni.isEmpty()) {
            iscrizioneRepository.deleteAll(iscrizioni);
        }

        corsoRepository.deleteById(id);
    }

    @Transactional
    public CorsoFormazione salvaCorso(CorsoFormazione corso) {
        if (corso.getDataOrario() != null && corso.getDataOrario().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Non puoi programmare un corso nel passato.");
        }

        boolean isNuovo = (corso.getIdCorso() == null);

        if (corso.getStato() == null) {
            corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);
        }
        CorsoFormazione salvato = corsoRepository.save(corso);

        if (isNuovo) {
            logService.registraEvento(
                    "Programmazione nuovo corso",
                    true,
                    "Creato corso '" + salvato.getTitolo() + "' (ID: " + salvato.getIdCorso() + "). Assegnato al docente ID: " + salvato.getDocente().getIdUtente()
            );

            String messaggioAssegnazione = "Ti è stato assegnato un nuovo corso: " + salvato.getTitolo() + ".\n"
                    + "Data e Ora: " + salvato.getDataOrario() + "\n"
                    + "Luogo: " + salvato.getLuogoFisico() + "\n"
                    + "Accedi alla tua area riservata per gestire il materiale e le iscrizioni.";

            notificaService.inviaNotifica(
                    salvato.getDocente(),
                    "Ti è stato assegnato il corso: " + salvato.getTitolo(),
                    Notifica.Priorita.ALTA,
                    Notifica.CanaleNotifica.IN_APP
            );

            notificaService.inviaNotifica(
                    salvato.getDocente(),
                    messaggioAssegnazione,
                    Notifica.Priorita.ALTA,
                    Notifica.CanaleNotifica.EMAIL
            );
        }
        return salvato;
    }

    // Valida le transizioni di stato della Macchina a Stati Finiti (FSM) bloccando, ad esempio, l'avvio di corsi senza iscritti
    @Transactional
    public void aggiornaStato(Integer idCorso, CorsoFormazione.StatoCorso nuovoStato) {
        CorsoFormazione corso = corsoRepository.findById(idCorso)
                .orElseThrow(() -> new IllegalArgumentException("Errore di integrità: Corso non trovato."));

        if (nuovoStato == CorsoFormazione.StatoCorso.IN_SVOLGIMENTO) {
            long conteggioIscritti = iscrizioneRepository.findByCorsoIdCorso(idCorso).size();

            if (conteggioIscritti == 0) {
                throw new IllegalStateException("Violazione FSM: Impossibile passare allo stato IN_SVOLGIMENTO. Il corso non ha dipendenti iscritti.");
            }
        }

        corso.setStato(nuovoStato);
        CorsoFormazione salvato = corsoRepository.save(corso);

        if (salvato.getDocente() != null) {
            String msgBase = "Lo stato del corso '" + salvato.getTitolo() + "' è cambiato in: " + nuovoStato;

            notificaService.inviaNotifica(
                    salvato.getDocente(),
                    msgBase,
                    Notifica.Priorita.ALTA,
                    Notifica.CanaleNotifica.IN_APP
            );
            notificaService.inviaNotifica(
                    salvato.getDocente(),
                    msgBase + "<br>Accedi al portale per verificare i dettagli.",
                    Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.EMAIL
            );
        }

        if (nuovoStato == CorsoFormazione.StatoCorso.IN_SVOLGIMENTO) {
            List<IscrizioneCorso> iscritti = iscrizioneRepository.findByCorsoIdCorso(idCorso);
            for (IscrizioneCorso isc : iscritti) {
                notificaService.inviaNotifica(
                        isc.getUtente(),
                        "Il corso '" + salvato.getTitolo() + "' è appena iniziato! Benvenuto in aula.",
                        Notifica.Priorita.MEDIA,
                        Notifica.CanaleNotifica.EMAIL
                );
            }
        }
    }

    // Chiude il ciclo didattico e innesca le notifiche automatiche (richiesta feedback studenti e alert validazione Admin)
    @Transactional
    public void concludiCorso(Integer idCorso) {
        CorsoFormazione corso = corsoRepository.findById(idCorso).orElseThrow();
        corso.setStato(CorsoFormazione.StatoCorso.CONCLUSO);
        corsoRepository.save(corso);

        List<IscrizioneCorso> partecipanti = iscrizioneRepository.findByCorsoAndPresenzaConfermataTrue(corso);

        for (IscrizioneCorso partecipante : partecipanti) {
            String messaggio = "Il corso '" + corso.getTitolo() + "' si è concluso. Ti invitiamo a lasciare un feedback qualitativo sulla piattaforma.";
            notificaService.inviaNotifica(
                    partecipante.getUtente(),
                    messaggio,
                    Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.IN_APP
            );

            notificaService.inviaNotifica(
                    partecipante.getUtente(),
                    messaggio, Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.EMAIL
            );
        }

        adminService.getUnicoAdmin().ifPresent(admin -> {
            String msgAdmin = "Il corso '" + corso.getTitolo() + "' è terminato. È richiesta la tua attenzione per la validazione delle presenze.";
            notificaService.inviaNotifica(
                    admin,
                    msgAdmin,
                    Notifica.Priorita.ALTA,
                    Notifica.CanaleNotifica.IN_APP
            );

            notificaService.inviaNotifica(
                    admin,
                    msgAdmin,
                    Notifica.Priorita.ALTA,
                    Notifica.CanaleNotifica.EMAIL
            );
        });
    }

    @Transactional(readOnly = true)
    public List<CorsoFormazione> trovaCorsiPerStato(CorsoFormazione.StatoCorso stato) {
        return corsoRepository.findByStato(stato);
    }

    // Cicla massivamente il registro presenze, validando gli studenti e mandando la pratica in firma digitale (ATTESA_FIRMA_DOCENTE)
    @Transactional
    public void validaPresenzeAdmin(Integer idCorso, List<Integer> idUtentiPresenti) {
        CorsoFormazione corso = corsoRepository.findById(idCorso)
                .orElseThrow(() -> new IllegalArgumentException("Errore di integrità: Corso non trovato"));

        if (corso.getStato() != CorsoFormazione.StatoCorso.CONCLUSO) {
            throw new IllegalStateException("Violazione FSM: Impossibile validare le presenze. Il corso si trova nello stato: " + corso.getStato());
        }

        List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByCorsoIdCorso(idCorso);

        for (IscrizioneCorso iscrizione : iscrizioni) {
            Integer idDipendente = iscrizione.getUtente().getIdUtente();

            if (idUtentiPresenti.contains(idDipendente)) {
                iscrizione.validaPresenza();
            } else {
                iscrizione.invalidaPresenza();
            }
        }

        iscrizioneRepository.saveAll(iscrizioni);

        corso.setStato(CorsoFormazione.StatoCorso.ATTESA_FIRMA_DOCENTE);
        corsoRepository.save(corso);

        int numeroPresentiValidati = idUtentiPresenti.size();
        logService.registraEvento(
                "Validazione registro didattico",
                true,
                "Corso ID: " + idCorso + " passato allo stato 'ATTESA_FIRMA_DOCENTE'. Validati " + numeroPresentiValidati + " presenti."
        );
    }

    // Implementa il controllo d'accesso (RBAC) per assicurare che solo il docente titolare possa controfirmare il registro
    @Transactional
    public void controfirmaDocente(Integer idCorso, Integer idDocente) {
        CorsoFormazione corso = corsoRepository.findById(idCorso)
                .orElseThrow(() -> new IllegalArgumentException("Errore di integrità: Corso non trovato"));

        if (!corso.getDocente().getIdUtente().equals(idDocente)) {
            throw new SecurityException("Violazione Accesso: Il docente specificato non è il titolare di questo corso.");
        }

        if (corso.getStato() != CorsoFormazione.StatoCorso.ATTESA_FIRMA_DOCENTE) {
            throw new IllegalStateException("Violazione FSM: Impossibile apporre la firma. Il corso si trova nello stato: " + corso.getStato());
        }

        corso.setStato(CorsoFormazione.StatoCorso.VALIDATO);
        corsoRepository.save(corso);
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