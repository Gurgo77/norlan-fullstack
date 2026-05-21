package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DipendenteDTO;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Livello di servizio (Business Logic) per la gestione dell'anagrafica Dipendenti.
 * Coordina l'inserimento, la validazione formale e garantisce rigorosamente l'integrità referenziale del database
 * attraverso procedure complesse di cancellazione manuale a cascata (Manual Cascade Delete).
 */

@Service
public class DipendenteService {

    @Autowired
    private DipendenteRepository dipendenteRepository;

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private AssegnazioneDPIRepository dpiRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Autowired
    private NotificaRepository notificaRepository;

    @Autowired
    private MessaggioRepository messaggioRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogSincronizzazioneService logService;

    @Transactional(readOnly = true)
    public List<Dipendente> findByAzienda(Integer aziendaId) {
        return dipendenteRepository.findByAziendaIdUtente(aziendaId);
    }

    @Transactional(readOnly = true)
    public List<Dipendente> findAll() {
        return dipendenteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Dipendente> findById(Integer id) {
        return dipendenteRepository.findById(id);
    }

    // Esegue un "Manual Cascade Delete": rimuove in modo sicuro tutte le dipendenze (DPI, iscrizioni, chat, feedback) prima di eliminare il record padre
    @Transactional
    public void eliminaDipendente(Integer id) {
        Dipendente dip = dipendenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lavoratore non trovato"));

        String nomeCompleto = dip.getNome() + " " + dip.getCognome();
        String ragioneSociale = dip.getAzienda().getRagioneSociale();

        dpiRepository.deleteAll(dpiRepository.findByDipendenteIdUtente(id));

        feedbackRepository.deleteAll(feedbackRepository.findAll().stream()
                .filter(f -> f.getIscrizione().getId().getIdUtente().equals(id))
                .toList());

        iscrizioneRepository.deleteAll(iscrizioneRepository.findByUtenteIdUtente(id));

        notificaRepository.deleteAll(notificaRepository.findByDestinatarioIdUtenteOrderByDataInvioDesc(id));

        messaggioRepository.deleteAll(messaggioRepository.findAll().stream()
                .filter(m -> m.getMittente().getIdUtente().equals(id) || m.getDestinatario().getIdUtente().equals(id))
                .toList());

        dipendenteRepository.deleteById(id);

        logService.registraEvento(
                "Eliminazione anagrafica: DIPENDENTE",
                true,
                "Cancellato dipendente ID: " + id + " (" + nomeCompleto + ") appartenente all'Azienda: " + ragioneSociale
        );
    }

    // Associa il lavoratore all'azienda madre, valida il formato del Codice Fiscale e applica l'hashing crittografico alle credenziali
    @Transactional
    public Dipendente salvaDipendente(Dipendente dipendente, Integer aziendaId) {
        boolean isNuovo = (dipendente.getIdUtente() == null);

        return aziendaRepository.findById(aziendaId).map(azienda -> {
            dipendente.setAzienda(azienda);

            if (dipendente.getCodiceFiscale() != null && dipendente.getCodiceFiscale().length() != 16) {
                throw new IllegalArgumentException("Codice Fiscale non valido.");
            }

            if (dipendente.getPasswordHash() != null) {
                dipendente.setPasswordHash(passwordEncoder.encode(dipendente.getPasswordHash()));
            }

            Dipendente salvato = dipendenteRepository.save(dipendente);

            if (isNuovo) {
                logService.registraEvento(
                        "Registrazione nuova anagrafica: DIPENDENTE",
                        true,
                        "Creato dipendente '" + salvato.getNome() + " " + salvato.getCognome() + "'. ID assegnato: " + salvato.getIdUtente() + " (Azienda ID: " + aziendaId + ")"
                );
            }
            return salvato;

        }).orElseThrow(() -> new RuntimeException("Azienda non trovata con ID: " + aziendaId));
    }

    // Appiattisce (flattening) la gerarchia della classe estraendo i dati essenziali dell'azienda collegata per un rapido consumo lato client
    public DipendenteDTO convertToDTO(Dipendente dipendente) {
        DipendenteDTO dto = new DipendenteDTO();
        dto.setIdUtente(dipendente.getIdUtente());
        dto.setEmail(dipendente.getEmail());
        dto.setRuolo(dipendente.getRuolo());
        dto.setNome(dipendente.getNome());
        dto.setCognome(dipendente.getCognome());
        dto.setCodiceFiscale(dipendente.getCodiceFiscale());

        if (dipendente.getAzienda() != null) {
            dto.setIdAzienda(dipendente.getAzienda().getIdUtente());
            dto.setRagioneSocialeAzienda(dipendente.getAzienda().getRagioneSociale());
        }
        return dto;
    }
}
