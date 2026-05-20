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

/**
 * Livello di servizio (Business Logic) per la gestione dell'Amministratore di sistema.
 * Coordina le transazioni sul database (@Transactional) e applica un vincolo architetturale
 * rigoroso per garantire l'esistenza di un unico super-utente globale (Pattern Singleton).
 */

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<Admin> getUnicoAdmin() {
        return adminRepository.findAll().stream().findFirst();
    }

    // Valida l'unicità dell'amministratore e ne registra le credenziali applicando l'hashing crittografico alla password
    @Transactional
    public Admin salvaAdmin(Admin admin) {
        if (adminRepository.count() >= 1) {
            throw new IllegalStateException("Configurazione negata: Il sistema prevede un unico Amministratore globale.");
        }

        admin.setRuolo(Utente.Ruolo.ADMIN);

        if (admin.getPasswordHash() != null) {
            admin.setPasswordHash(passwordEncoder.encode(admin.getPasswordHash()));
        }

        return adminRepository.save(admin);
    }

    @Transactional
    public Admin aggiornaAdmin(Integer id, AdminDTO dto) {
        Admin adminEsistente = adminRepository.findById(id).orElseThrow(() -> new RuntimeException("Amministratore non trovato"));

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            adminEsistente.setEmail(dto.getEmail());
        }

        return adminRepository.save(adminEsistente);
    }

    // Mappa l'entità di database nel rispettivo Data Transfer Object (DTO) per un'esposizione sicura verso il client
    public AdminDTO convertToDTO(Admin admin) {
        AdminDTO dto = new AdminDTO();

        dto.setIdUtente(admin.getIdUtente());
        dto.setEmail(admin.getEmail());
        dto.setRuolo(admin.getRuolo());

        return dto;
    }
}
