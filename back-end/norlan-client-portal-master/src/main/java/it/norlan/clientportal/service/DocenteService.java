package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DocenteDTO;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.repository.DocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Livello di servizio (Business Logic) per la gestione dell'anagrafica Docenti (Formatori).
 * Regola l'inserimento assicurando la presenza dei requisiti tecnici obbligatori (specializzazione)
 * e traccia le operazioni critiche di rimozione tramite il sistema di auditing centralizzato.
 */

@Service
public class DocenteService {

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogSincronizzazioneService logService;

    @Transactional(readOnly = true)
    public List<Docente> findAll() {
        return docenteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Docente> findById(Integer id) {
        return docenteRepository.findById(id);
    }

    // Valida la presenza della specializzazione tecnica obbligatoria e applica l'hashing sicuro alle credenziali di accesso
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

    // Rimuove l'anagrafica del formatore dal database e registra tempestivamente l'operazione nei log di sicurezza (Auditing)
    @Transactional
    public void eliminaDocente(Integer id) {
        Docente docente = docenteRepository.findById(id).orElseThrow();
        String nomeCompleto = docente.getNome() + " " + docente.getCognome();
        docenteRepository.deleteById(id);
        logService.registraEvento(
                "Eliminazione anagrafica: DOCENTE",
                true,
                "Cancellato docente ID: " + id + " (" + nomeCompleto + ")"
        );
    }

    // Mappa l'entità di database nel DTO corrispondente per garantire un trasferimento dati sicuro ed efficiente verso il client
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
