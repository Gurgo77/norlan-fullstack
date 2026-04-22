package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DocenteDTO;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.repository.DocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DocenteService {

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        // Validazione: un docente deve avere almeno una specializzazione indicata
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
        docenteRepository.deleteById(id);
    }

    public DocenteDTO convertToDTO(Docente docente) {
        DocenteDTO dto = new DocenteDTO();

        // Travaso dati comuni (ereditati)
        dto.setIdUtente(docente.getIdUtente());
        dto.setNome(docente.getNome());
        dto.setCognome(docente.getCognome());
        dto.setEmail(docente.getEmail());
        dto.setRuolo(docente.getRuolo());

        // Travaso dato specifico
        dto.setSpecializzazioneTecnica(docente.getSpecializzazioneTecnica());

        return dto;
    }
}
