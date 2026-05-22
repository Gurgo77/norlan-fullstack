package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.UtenteDTO;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di Identity & Access Management (IAM).
 * Verifica l'orchestrazione del ciclo di vita delle credenziali, la corretta delega
 * crittografica al framework di sicurezza e le policy transazionali di rotazione delle password.
 */
@ExtendWith(MockitoExtension.class)
class UtenteServiceTest {

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificaService notificaService;

    @InjectMocks
    private UtenteService utenteService;

    private Utente utente;

    @BeforeEach
    void setUp() {

        utente = new Utente() {};
        utente.setIdUtente(1);
        utente.setEmail("test@norlan.it");
        utente.setPasswordHash("password_codificata");
        utente.setRuolo(Utente.Ruolo.DIPENDENTE);
        utente.setRichiedeCambioPassword(true);
    }

    @Test
    void findByEmail_Trovato_RitornaOptionalPresente() {
        when(utenteRepository.findByEmail("test@norlan.it")).thenReturn(Optional.of(utente));

        Optional<Utente> result = utenteService.findByEmail("test@norlan.it");

        assertTrue(result.isPresent());
        assertEquals("test@norlan.it", result.get().getEmail());
    }

    @Test
    void findByEmail_NonTrovato_RitornaOptionalVuoto() {
        when(utenteRepository.findByEmail("inesistente@norlan.it")).thenReturn(Optional.empty());

        Optional<Utente> result = utenteService.findByEmail("inesistente@norlan.it");

        assertTrue(result.isEmpty());
    }

    // Cryptographic Delegation (Separation of Concerns): assicura che il layer di business demandi tassativamente il calcolo dell'hash al provider di sicurezza, prevenendo leak di credenziali in chiaro
    @Test
    void registraUtente_Successo_CodificaPasswordESalva() {
        Utente nuovoUtente = new Utente() {};
        nuovoUtente.setPasswordHash("password_in_chiaro");

        when(passwordEncoder.encode("password_in_chiaro")).thenReturn("nuova_password_codificata");
        when(utenteRepository.save(any(Utente.class))).thenAnswer(i -> i.getArgument(0));

        Utente result = utenteService.registraUtente(nuovoUtente);

        assertEquals("nuova_password_codificata", result.getPasswordHash());
        verify(passwordEncoder).encode("password_in_chiaro");
        verify(utenteRepository).save(nuovoUtente);
    }

    @Test
    void verificaCredenziali_EmailEsistentePasswordCorretta_RitornaTrue() {
        when(utenteRepository.findByEmail("test@norlan.it")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("password_giusta", "password_codificata")).thenReturn(true);

        boolean result = utenteService.verificaCredenziali("test@norlan.it", "password_giusta");

        assertTrue(result);
    }

    @Test
    void verificaCredenziali_EmailEsistentePasswordErrata_RitornaFalse() {
        when(utenteRepository.findByEmail("test@norlan.it")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("password_sbagliata", "password_codificata")).thenReturn(false);

        boolean result = utenteService.verificaCredenziali("test@norlan.it", "password_sbagliata");

        assertFalse(result);
    }

    // Resource Optimization (Short-Circuit Evaluation): verifica l'uscita anticipata dal flusso (Fail-Fast) per inibire l'esecuzione di algoritmi crittografici onerosi (es. BCrypt) su identità non censite, risparmiando cicli CPU
    @Test
    void verificaCredenziali_EmailNonEsistente_RitornaFalse() {
        when(utenteRepository.findByEmail("inesistente@norlan.it")).thenReturn(Optional.empty());

        boolean result = utenteService.verificaCredenziali("inesistente@norlan.it", "password_qualsiasi");

        assertFalse(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void cambiaPassword_UtenteNonTrovato_LanciaEccezione() {
        when(utenteRepository.findByEmail("inesistente@norlan.it")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> utenteService.cambiaPassword("inesistente@norlan.it", "vecchia", "nuova"));

        verify(utenteRepository, never()).save(any());
    }

    // Zero-Trust Security (Guard Clause): collauda il blocco architetturale durante la rotazione delle credenziali, garantendo che la mutazione avvenga solo previa dimostrazione crittografica del vecchio segreto
    @Test
    void cambiaPassword_VecchiaPasswordErrata_LanciaBadCredentialsException() {
        when(utenteRepository.findByEmail("test@norlan.it")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("vecchia_sbagliata", "password_codificata")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> utenteService.cambiaPassword("test@norlan.it", "vecchia_sbagliata", "nuova"));

        verify(utenteRepository, never()).save(any());
    }

    // State Mutation & Security Alerting: verifica l'aggiornamento transazionale dello stato (reset flag di scadenza) e il contestuale innesco del workflow di notifica (Audit Trail) a tutela dell'account
    @Test
    void cambiaPassword_Successo_AggiornaSalvaENotifica() {
        when(utenteRepository.findByEmail("test@norlan.it")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("vecchia_giusta", "password_codificata")).thenReturn(true);
        when(passwordEncoder.encode("nuova_sicura")).thenReturn("nuova_codificata");

        utenteService.cambiaPassword("test@norlan.it", "vecchia_giusta", "nuova_sicura");

        assertEquals("nuova_codificata", utente.getPasswordHash());
        assertFalse(utente.getRichiedeCambioPassword());
        verify(utenteRepository).save(utente);
        verify(notificaService).inviaNotifica(
                eq(utente),
                anyString(),
                eq(Notifica.Priorita.ALTA),
                eq(Notifica.CanaleNotifica.EMAIL)
        );
    }

    @Test
    void convertToDTO_MappaCampiCorrettamente() {
        UtenteDTO dto = utenteService.convertToDTO(utente);

        assertEquals(1, dto.getIdUtente());
        assertEquals("test@norlan.it", dto.getEmail());
        assertEquals(Utente.Ruolo.DIPENDENTE, dto.getRuolo());
        assertEquals("DIPENDENTE", dto.getTipoUtente());
    }
}
