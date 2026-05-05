package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.UtenteDTO;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UtenteService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificaService notificaService;

    @Transactional(readOnly = true)
    public Optional<Utente> findByEmail(String email) {
        return utenteRepository.findByEmail(email);
    }

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

    @Transactional
    public void cambiaPassword(String email, String vecchiaPassword, String nuovaPassword) {
        Utente utente = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        if (!passwordEncoder.matches(vecchiaPassword, utente.getPasswordHash())) {
            throw new BadCredentialsException("La vecchia password non è corretta");
        }

        utente.setPasswordHash(passwordEncoder.encode(nuovaPassword));
        utente.setRichiedeCambioPassword(false);
        utenteRepository.save(utente);

        String messaggioSicurezza = "Ti informiamo che la password del tuo account NorLan è stata appena modificata con successo.<br><br>"
                + "Se sei stato tu ad effettuare questa operazione, puoi ignorare questa email.<br><br>"
                + "<b>Se NON hai richiesto questa modifica</b>, ti invitiamo a contattare immediatamente l'amministratore di sistema o il supporto tecnico.";

        notificaService.inviaNotifica(
                utente,
                messaggioSicurezza,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.EMAIL
        );
    }

    public UtenteDTO convertToDTO(Utente utente) {
        UtenteDTO dto = new UtenteDTO();
        dto.setIdUtente(utente.getIdUtente());
        dto.setEmail(utente.getEmail());
        dto.setRuolo(utente.getRuolo());
        dto.setTipoUtente(utente.getRuolo().name());

        return dto;
    }
}
