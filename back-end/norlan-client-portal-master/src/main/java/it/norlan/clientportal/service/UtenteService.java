package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.UtenteDTO;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UtenteService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Utente registraUtente(Utente utente) {

        String passwordHash = passwordEncoder.encode(utente.getPasswordHash());
        utente.setPasswordHash(passwordHash);


        return utenteRepository.save(utente);
    }

    public boolean verificaCredenziali(String email, String passwordChiara) {
        Optional<Utente> utenteOpt = utenteRepository.findByEmail(email);

        if (utenteOpt.isPresent()) {

            return passwordEncoder.matches(passwordChiara, utenteOpt.get().getPasswordHash());
        }
        return false;
    }

    public UtenteDTO convertToDTO(Utente utente) {
        UtenteDTO dto = new UtenteDTO();
        dto.setIdUtente(utente.getIdUtente());
        dto.setEmail(utente.getEmail());
        dto.setRuolo(utente.getRuolo());

        // Possiamo impostare una stringa leggibile per il frontend
        dto.setTipoUtente(utente.getRuolo().name());

        return dto;
    }
}
