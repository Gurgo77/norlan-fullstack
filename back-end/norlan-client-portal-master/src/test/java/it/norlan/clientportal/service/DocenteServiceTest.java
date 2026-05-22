package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DocenteDTO;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.DocenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di Business Logic del dominio Didattico (Docente).
 * Verifica le policy di validazione stringente sulle competenze tecniche (Sanitizzazione Input),
 * l'orchestrazione crittografica delle password e l'integrità dei log di Audit.
 */
@ExtendWith(MockitoExtension.class)
class DocenteServiceTest {

    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private DocenteService docenteService;

    private Docente docente;

    @BeforeEach
    void setUp() {
        docente = new Docente();
        docente.setIdUtente(1);
        docente.setNome("Luigi");
        docente.setCognome("Verdi");
        docente.setEmail("luigi.verdi@test.it");
        docente.setRuolo(Utente.Ruolo.DOCENTE);
        docente.setSpecializzazioneTecnica("Sicurezza sul lavoro");
    }

    @Test
    void findAll_RitornaListaCompleta() {
        when(docenteRepository.findAll()).thenReturn(List.of(docente));

        List<Docente> result = docenteService.findAll();

        assertEquals(1, result.size());
        assertEquals("Luigi", result.get(0).getNome());
    }

    @Test
    void findById_Trovato_RitornaOptionalPresente() {
        when(docenteRepository.findById(1)).thenReturn(Optional.of(docente));

        Optional<Docente> result = docenteService.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Luigi", result.get().getNome());
    }

    @Test
    void findById_NonTrovato_RitornaOptionalVuoto() {
        when(docenteRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Docente> result = docenteService.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void salvaDocente_SpecializzazioneNull_LanciaIllegalArgumentException() {
        docente.setSpecializzazioneTecnica(null);

        assertThrows(IllegalArgumentException.class, () -> docenteService.salvaDocente(docente));
        verify(docenteRepository, never()).save(any());
    }

    // Sanitizzazione e Guard Clauses: verifica che il dominio rifiuti input logicamente vuoti (Blank strings), proteggendo il database da anomalie semantiche silenziose
    @Test
    void salvaDocente_SpecializzazioneVuota_LanciaIllegalArgumentException() {
        docente.setSpecializzazioneTecnica("   ");

        assertThrows(IllegalArgumentException.class, () -> docenteService.salvaDocente(docente));
        verify(docenteRepository, never()).save(any());
    }

    // Security Delegation: assicura che il Service funga da orchestratore sicuro, demandando tassativamente l'hashing asimmetrico al componente dedicato (PasswordEncoder) prima della persistenza
    @Test
    void salvaDocente_ConPassword_CodificaESalva() {
        docente.setPasswordHash("passwordSegreta");
        when(passwordEncoder.encode("passwordSegreta")).thenReturn("passwordCodificata");
        when(docenteRepository.save(any(Docente.class))).thenReturn(docente);

        Docente result = docenteService.salvaDocente(docente);

        assertEquals("passwordCodificata", result.getPasswordHash());
        verify(passwordEncoder).encode("passwordSegreta");
        verify(docenteRepository).save(docente);
    }

    @Test
    void salvaDocente_SenzaPassword_SalvaSenzaCodifica() {
        docente.setPasswordHash(null);
        when(docenteRepository.save(any(Docente.class))).thenReturn(docente);

        Docente result = docenteService.salvaDocente(docente);

        assertNull(result.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
        verify(docenteRepository).save(docente);
    }

    // Fail-Fast & Audit Integrity: collauda il blocco immediato in caso di entità inesistente, verificando esplicitamente che il sistema non generi falsi log di cancellazione (Audit Pollution)
    @Test
    void eliminaDocente_DocenteNonTrovato_LanciaEccezione() {
        when(docenteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> docenteService.eliminaDocente(99));
        verify(docenteRepository, never()).deleteById(anyInt());
        verify(logService, never()).registraEvento(anyString(), anyBoolean(), anyString());
    }

    @Test
    void eliminaDocente_DocenteTrovato_EliminaELogga() {
        when(docenteRepository.findById(1)).thenReturn(Optional.of(docente));

        docenteService.eliminaDocente(1);

        verify(docenteRepository).deleteById(1);
        verify(logService).registraEvento(
                eq("Eliminazione anagrafica: DOCENTE"),
                eq(true),
                eq("Cancellato docente ID: 1")
        );
    }

    @Test
    void convertToDTO_MappaCorrettamente() {
        DocenteDTO dto = docenteService.convertToDTO(docente);

        assertEquals(1, dto.getIdUtente());
        assertEquals("Luigi", dto.getNome());
        assertEquals("Verdi", dto.getCognome());
        assertEquals("luigi.verdi@test.it", dto.getEmail());
        assertEquals(Utente.Ruolo.DOCENTE, dto.getRuolo());
        assertEquals("Sicurezza sul lavoro", dto.getSpecializzazioneTecnica());
    }
}
