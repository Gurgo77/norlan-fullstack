package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.LogSincronizzazioneDTO;
import it.norlan.clientportal.model.LogSincronizzazione;
import it.norlan.clientportal.repository.LogSincronizzazioneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogSincronizzazioneServiceTest {

    @Mock
    private LogSincronizzazioneRepository repository;

    @InjectMocks
    private LogSincronizzazioneService logService;

    private LogSincronizzazione logEvent;

    @BeforeEach
    void setUp() {
        logEvent = new LogSincronizzazione();
        logEvent.setIdLog(1);
        logEvent.setDescrizioneEvento("Sincronizzazione Anagrafiche");
        logEvent.setEsitoPositivo(true);
        logEvent.setNoteTecniche("Importati 15 nuovi record.");
        logEvent.setDataEvento(LocalDateTime.of(2026, 5, 6, 10, 0));
    }

    @Test
    void findAll_RitornaListaOrdinata() {
        when(repository.findAllByOrderByDataEventoDesc()).thenReturn(List.of(logEvent));

        List<LogSincronizzazione> result = logService.findAll();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(repository).findAllByOrderByDataEventoDesc();
    }

    @Test
    void registraEvento_DatiValidi_SalvaLogConDataOdierna() {
        when(repository.save(any(LogSincronizzazione.class))).thenAnswer(i -> {
            LogSincronizzazione saved = i.getArgument(0);
            saved.setIdLog(2);
            return saved;
        });

        LogSincronizzazione result = logService.registraEvento(
                "Aggiornamento Permessi",
                false,
                "Errore di connessione al database."
        );

        assertNotNull(result);
        assertEquals(2, result.getIdLog());

        ArgumentCaptor<LogSincronizzazione> captor = ArgumentCaptor.forClass(LogSincronizzazione.class);
        verify(repository).save(captor.capture());

        LogSincronizzazione captured = captor.getValue();
        assertEquals("Aggiornamento Permessi", captured.getDescrizioneEvento());
        assertFalse(captured.getEsitoPositivo());
        assertEquals("Errore di connessione al database.", captured.getNoteTecniche());
        assertNotNull(captured.getDataEvento());
    }

    @Test
    void trovaErrori_RitornaSoloLogNegativi() {
        LogSincronizzazione logErrore = new LogSincronizzazione();
        logErrore.setEsitoPositivo(false);

        when(repository.findByEsitoPositivoFalse()).thenReturn(List.of(logErrore));

        List<LogSincronizzazione> result = logService.trovaErrori();

        assertEquals(1, result.size());
        assertFalse(result.get(0).getEsitoPositivo());
        verify(repository).findByEsitoPositivoFalse();
    }

    @Test
    void pulisciLogVecchi_EsegueCancellazione() {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(60);

        logService.pulisciLogVecchi(dataLimite);

        verify(repository).deleteByDataEventoBefore(dataLimite);
    }

    @Test
    void esecuzionePuliziaAutomatica_EsegueCancellazioneERegistraEvento() {
        logService.esecuzionePuliziaAutomatica();

        verify(repository).deleteByDataEventoBefore(any(LocalDateTime.class));

        ArgumentCaptor<LogSincronizzazione> captor = ArgumentCaptor.forClass(LogSincronizzazione.class);
        verify(repository).save(captor.capture());

        LogSincronizzazione savedLog = captor.getValue();
        assertEquals("Manutenzione di Sistema", savedLog.getDescrizioneEvento());
        assertTrue(savedLog.getEsitoPositivo());
        assertTrue(savedLog.getNoteTecniche().contains("Eseguita pulizia automatica"));
    }

    @Test
    void convertToDTO_MappaTuttiICampiCorrettamente() {
        LogSincronizzazioneDTO dto = logService.convertToDTO(logEvent);

        assertEquals(1, dto.getIdLog());
        assertEquals("Sincronizzazione Anagrafiche", dto.getDescrizioneEvento());
        assertEquals(LocalDateTime.of(2026, 5, 6, 10, 0), dto.getDataEvento());
        assertTrue(dto.getEsitoPositivo());
        assertEquals("Importati 15 nuovi record.", dto.getNoteTecniche());
    }
}
