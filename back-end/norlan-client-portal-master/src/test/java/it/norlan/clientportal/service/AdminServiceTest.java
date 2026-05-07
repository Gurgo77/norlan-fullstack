package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.AdminDTO;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getUnicoAdmin_Esiste_RitornaAdmin() {
        Admin admin = new Admin();
        admin.setIdUtente(1);
        when(adminRepository.findAll()).thenReturn(List.of(admin));

        Optional<Admin> result = adminService.getUnicoAdmin();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getIdUtente());
    }

    @Test
    void getUnicoAdmin_NonEsiste_RitornaEmpty() {
        when(adminRepository.findAll()).thenReturn(List.of());

        Optional<Admin> result = adminService.getUnicoAdmin();

        assertTrue(result.isEmpty());
    }

    @Test
    void salvaAdmin_CountZeroConPassword_SalvaCorrettamente() {
        when(adminRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        Admin inputAdmin = new Admin();
        inputAdmin.setPasswordHash("rawPassword");

        Admin savedAdmin = new Admin();
        savedAdmin.setIdUtente(1);
        savedAdmin.setRuolo(Utente.Ruolo.ADMIN);
        savedAdmin.setPasswordHash("encodedPassword");

        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);

        Admin result = adminService.salvaAdmin(inputAdmin);

        assertNotNull(result);
        assertEquals(Utente.Ruolo.ADMIN, result.getRuolo());
        assertEquals("encodedPassword", result.getPasswordHash());
        verify(adminRepository).save(inputAdmin);
    }

    @Test
    void salvaAdmin_CountZeroSenzaPassword_SalvaCorrettamente() {
        when(adminRepository.count()).thenReturn(0L);

        Admin inputAdmin = new Admin();
        inputAdmin.setPasswordHash(null);

        Admin savedAdmin = new Admin();
        savedAdmin.setRuolo(Utente.Ruolo.ADMIN);

        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);

        Admin result = adminService.salvaAdmin(inputAdmin);

        assertNotNull(result);
        assertEquals(Utente.Ruolo.ADMIN, result.getRuolo());
        verify(passwordEncoder, never()).encode(anyString());
        verify(adminRepository).save(inputAdmin);
    }

    @Test
    void salvaAdmin_AdminEsistente_LanciaIllegalStateException() {
        when(adminRepository.count()).thenReturn(1L);

        Admin inputAdmin = new Admin();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminService.salvaAdmin(inputAdmin)
        );

        assertEquals("Configurazione negata: Il sistema prevede un unico admin globale.", exception.getMessage());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void aggiornaAdmin_TrovatoEmailValida_AggiornaESalva() {
        Admin adminEsistente = new Admin();
        adminEsistente.setIdUtente(1);
        adminEsistente.setEmail("old@email.it");

        AdminDTO dto = new AdminDTO();
        dto.setEmail("new@email.it");

        when(adminRepository.findById(1)).thenReturn(Optional.of(adminEsistente));
        when(adminRepository.save(any(Admin.class))).thenReturn(adminEsistente);

        Admin result = adminService.aggiornaAdmin(1, dto);

        assertNotNull(result);
        assertEquals("new@email.it", result.getEmail());
        verify(adminRepository).save(adminEsistente);
    }

    @Test
    void aggiornaAdmin_TrovatoEmailNull_SalvaSenzaModifiche() {
        Admin adminEsistente = new Admin();
        adminEsistente.setIdUtente(1);
        adminEsistente.setEmail("old@email.it");

        AdminDTO dto = new AdminDTO();
        dto.setEmail(null);

        when(adminRepository.findById(1)).thenReturn(Optional.of(adminEsistente));
        when(adminRepository.save(any(Admin.class))).thenReturn(adminEsistente);

        Admin result = adminService.aggiornaAdmin(1, dto);

        assertNotNull(result);
        assertEquals("old@email.it", result.getEmail());
        verify(adminRepository).save(adminEsistente);
    }

    @Test
    void aggiornaAdmin_NonTrovato_LanciaRuntimeException() {
        when(adminRepository.findById(1)).thenReturn(Optional.empty());

        AdminDTO dto = new AdminDTO();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adminService.aggiornaAdmin(1, dto)
        );

        assertEquals("Admin non trovato", exception.getMessage());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void convertToDTO_MappaCampiCorrettamente() {
        Admin admin = new Admin();
        admin.setIdUtente(10);
        admin.setEmail("admin@test.it");
        admin.setRuolo(Utente.Ruolo.ADMIN);

        AdminDTO dto = adminService.convertToDTO(admin);

        assertNotNull(dto);
        assertEquals(10, dto.getIdUtente());
        assertEquals("admin@test.it", dto.getEmail());
        assertEquals(Utente.Ruolo.ADMIN, dto.getRuolo());
    }
}
