package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DipendenteDTO;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.repository.DipendenteRepository;
import it.norlan.clientportal.repository.AziendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DipendenteService {

    @Autowired
    private DipendenteRepository dipendenteRepository;

    @Autowired
    private AziendaRepository aziendaRepository;

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

    @Transactional
    public void eliminaDipendente(Integer id) {

        Dipendente dip = dipendenteRepository.findById(id).orElseThrow();
        String nomeCompleto = dip.getNome() + " " + dip.getCognome();
        String ragioneSociale = dip.getAzienda().getRagioneSociale();

        dipendenteRepository.deleteById(id);

        logService.registraEvento(
                "Eliminazione anagrafica: DIPENDENTE",
                true,
                "Cancellato dipendente ID: " + id + " (" + nomeCompleto + ") appartenente all'Azienda: " + ragioneSociale
        );
    }

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

    public DipendenteDTO convertToDTO(Dipendente dipendente) {
        DipendenteDTO dto = new DipendenteDTO();

        // Campi base
        dto.setIdUtente(dipendente.getIdUtente());
        dto.setEmail(dipendente.getEmail());
        dto.setRuolo(dipendente.getRuolo());
        dto.setNome(dipendente.getNome());
        dto.setCognome(dipendente.getCognome());
        dto.setCodiceFiscale(dipendente.getCodiceFiscale());

        // Estrazione dati dall'azienda collegata
        if (dipendente.getAzienda() != null) {
            dto.setIdAzienda(dipendente.getAzienda().getIdUtente());
            dto.setRagioneSocialeAzienda(dipendente.getAzienda().getRagioneSociale());
        }

        return dto;
    }
}
