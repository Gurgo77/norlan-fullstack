package it.norlan.clientportal.security;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UtenteRepository utenteRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void loadUserByUsername_UtenteTrovato_RitornaUserDetails() {
        Admin utente = new Admin();
        utente.setEmail("admin@norlan.it");
        utente.setPasswordHash("hashed_pwd_123");
        utente.setRuolo(Utente.Ruolo.ADMIN);

        when(utenteRepository.findByEmail("admin@norlan.it")).thenReturn(Optional.of(utente));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@norlan.it");

        assertNotNull(userDetails);
        assertEquals("admin@norlan.it", userDetails.getUsername());
        assertEquals("hashed_pwd_123", userDetails.getPassword());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_UtenteNonTrovato_LanciaEccezione() {
        when(utenteRepository.findByEmail("sconosciuto@norlan.it")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("sconosciuto@norlan.it")
        );

        assertEquals("Utente non trovato con email: sconosciuto@norlan.it", exception.getMessage());
    }
}
