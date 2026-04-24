package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.AdminDTO;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Ritorna l'unico amministratore del sistema (se presente).
     */
    @Transactional(readOnly = true)
    public Optional<Admin> getUnicoAdmin() {
        return adminRepository.findAll().stream().findFirst();
    }

    /**
     * Registra l'unico amministratore.
     * Se ne esiste già uno, lancia un'eccezione.
     */
    @Transactional
    public Admin salvaAdmin(Admin admin) {
        // Controllo scientifico di esistenza (Cardinalità 1:1)
        if (adminRepository.count() >= 1) {
            throw new IllegalStateException("Configurazione negata: Il sistema prevede un unico Amministratore globale.");
        }

        // Configurazione sicurezza
        admin.setRuolo(Utente.Ruolo.ADMIN);

        if (admin.getPasswordHash() != null) {
            admin.setPasswordHash(passwordEncoder.encode(admin.getPasswordHash()));
        }

        return adminRepository.save(admin);
    }

    /**
     * Permette di aggiornare i dati dell'unico admin esistente senza crearne di nuovi.
     */
    @Transactional
    public Admin aggiornaAdmin(Integer id, AdminDTO dto) {
        // 1. Recupera l'entità esistente dal database
        Admin adminEsistente = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amministratore non trovato"));

        // 2. Aggiorna SOLO i campi consentiti (nel nostro caso, solo l'email)
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            adminEsistente.setEmail(dto.getEmail());
        }

        // 3. Salva e restituisci l'entità aggiornata
        return adminRepository.save(adminEsistente);
    }

    public AdminDTO convertToDTO(Admin admin) {
        AdminDTO dto = new AdminDTO();

        dto.setIdUtente(admin.getIdUtente());
        dto.setEmail(admin.getEmail());
        dto.setRuolo(admin.getRuolo());

        return dto;
    }
}
