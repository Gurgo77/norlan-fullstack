package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.DipendenteDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DipendenteServiceTest {

    @Mock
    private DipendenteRepository dipendenteRepository;
    @Mock
    private AziendaRepository aziendaRepository;
    @Mock
    private AssegnazioneDPIRepository dpiRepository;
    @Mock
    private IscrizioneCorsoRepository iscrizioneRepository;
    @Mock
    private NotificaRepository notificaRepository;
    @Mock
    private MessaggioRepository messaggioRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private DipendenteService dipendenteService;

    private Dipendente dipendente;
    private Azienda azienda;

    @BeforeEach
    void setUp() {
        azienda = new Azienda();
        azienda.setIdUtente(1);
        azienda.setRagioneSociale("Azienda Test Spa");

        dipendente = new Dipendente();
        dipendente.setIdUtente(10);
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setCodiceFiscale("RSSMRA80A01H501Z");
        dipendente.setEmail("mario.rossi@test.it");
        dipendente.setAzienda(azienda);
    }

    @Test
    void findByAzienda_RitornaLista() {
        when(dipendenteRepository.findByAziendaIdUtente(1)).thenReturn(List.of(dipendente));

        List<Dipendente> result = dipendenteService.findByAzienda(1);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void findAll_RitornaLista() {
        when(dipendenteRepository.findAll()).thenReturn(List.of(dipendente));

        List<Dipendente> result = dipendenteService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void findById_RitornaOptional() {
        when(dipendenteRepository.findById(10)).thenReturn(Optional.of(dipendente));

        Optional<Dipendente> result = dipendenteService.findById(10);

        assertTrue(result.isPresent());
        assertEquals("Mario", result.get().getNome());
    }

    @Test
    void eliminaDipendente_LavoratoreNonTrovato_LanciaEccezione() {
        when(dipendenteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> dipendenteService.eliminaDipendente(99));
        verify(dipendenteRepository, never()).deleteById(anyInt());
    }

    @Test
    void eliminaDipendente_Successo_PulisceRelazioniEdElimina() {

        when(dipendenteRepository.findById(10)).thenReturn(Optional.of(dipendente));
        when(dpiRepository.findByDipendenteIdUtente(10)).thenReturn(Collections.emptyList());
        when(feedbackRepository.findAll()).thenReturn(Collections.emptyList());
        when(iscrizioneRepository.findByUtenteIdUtente(10)).thenReturn(Collections.emptyList());
        when(notificaRepository.findByDestinatarioIdUtenteOrderByDataInvioDesc(10)).thenReturn(Collections.emptyList());
        when(messaggioRepository.findAll()).thenReturn(Collections.emptyList());

        dipendenteService.eliminaDipendente(10);

        verify(dpiRepository).deleteAll(anyList());
        verify(feedbackRepository).deleteAll(anyList());
        verify(iscrizioneRepository).deleteAll(anyList());
        verify(notificaRepository).deleteAll(anyList());
        verify(messaggioRepository).deleteAll(anyList());
        verify(dipendenteRepository).deleteById(10);
        verify(logService).registraEvento(eq("Eliminazione anagrafica: DIPENDENTE"), eq(true), anyString());
    }

    @Test
    void salvaDipendente_AziendaNonTrovata_LanciaEccezione() {
        when(aziendaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> dipendenteService.salvaDipendente(dipendente, 99));
        verify(dipendenteRepository, never()).save(any());
    }

    @Test
    void salvaDipendente_CodiceFiscaleInvalido_LanciaEccezione() {
        dipendente.setCodiceFiscale("CORT0");
        when(aziendaRepository.findById(1)).thenReturn(Optional.of(azienda));

        assertThrows(IllegalArgumentException.class, () -> dipendenteService.salvaDipendente(dipendente, 1));
        verify(dipendenteRepository, never()).save(any());
    }

    @Test
    void salvaDipendente_NuovoDipendenteConPassword_SalvaCodificaLogga() {
        dipendente.setIdUtente(null);
        dipendente.setPasswordHash("rawPassword123");

        when(aziendaRepository.findById(1)).thenReturn(Optional.of(azienda));
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encodedPassword");

        Dipendente salvato = new Dipendente();
        salvato.setIdUtente(11);
        salvato.setNome("Mario");
        salvato.setCognome("Rossi");

        when(dipendenteRepository.save(any(Dipendente.class))).thenReturn(salvato);

        Dipendente result = dipendenteService.salvaDipendente(dipendente, 1);

        assertEquals("encodedPassword", dipendente.getPasswordHash());
        assertEquals(11, result.getIdUtente());
        verify(dipendenteRepository).save(dipendente);
        verify(logService).registraEvento(eq("Registrazione nuova anagrafica: DIPENDENTE"), eq(true), anyString());
    }

    @Test
    void salvaDipendente_DipendenteEsistenteSenzaPassword_SalvaSenzaCodificareSenzaLog() {
        dipendente.setPasswordHash(null);

        when(aziendaRepository.findById(1)).thenReturn(Optional.of(azienda));
        when(dipendenteRepository.save(any(Dipendente.class))).thenReturn(dipendente);

        Dipendente result = dipendenteService.salvaDipendente(dipendente, 1);

        assertEquals(10, result.getIdUtente());
        verify(passwordEncoder, never()).encode(anyString());
        verify(dipendenteRepository).save(dipendente);
        verify(logService, never()).registraEvento(anyString(), anyBoolean(), anyString());
    }

    @Test
    void convertToDTO_ConAzienda_MappaTuttiICampi() {
        DipendenteDTO dto = dipendenteService.convertToDTO(dipendente);

        assertEquals(10, dto.getIdUtente());
        assertEquals("Mario", dto.getNome());
        assertEquals("RSSMRA80A01H501Z", dto.getCodiceFiscale());
        assertEquals(1, dto.getIdAzienda());
        assertEquals("Azienda Test Spa", dto.getRagioneSocialeAzienda());
    }

    @Test
    void convertToDTO_SenzaAzienda_MappaCampiBase() {
        dipendente.setAzienda(null);

        DipendenteDTO dto = dipendenteService.convertToDTO(dipendente);

        assertEquals(10, dto.getIdUtente());
        assertEquals("Mario", dto.getNome());
        assertNull(dto.getIdAzienda());
        assertNull(dto.getRagioneSocialeAzienda());
    }
}
