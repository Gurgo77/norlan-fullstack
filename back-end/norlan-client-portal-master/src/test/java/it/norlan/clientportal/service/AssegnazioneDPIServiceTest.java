package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.AssegnazioneDPIDTO;
import it.norlan.clientportal.model.AssegnazioneDPI;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.repository.AssegnazioneDPIRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssegnazioneDPIServiceTest {

    @Mock
    private AssegnazioneDPIRepository dpiRepository;

    @Mock
    private NotificaService notificaService;

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private AssegnazioneDPIService assegnazioneDPIService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void findAll_RitornaListaAssegnazioni() {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        when(dpiRepository.findAll()).thenReturn(List.of(dpi));

        List<AssegnazioneDPI> result = assegnazioneDPIService.findAll();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void findById_Trovato_RitornaOptionalPresente() {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        when(dpiRepository.findById(1)).thenReturn(Optional.of(dpi));

        Optional<AssegnazioneDPI> result = assegnazioneDPIService.findById(1);

        assertTrue(result.isPresent());
    }

    @Test
    void findById_NonTrovato_RitornaOptionalVuoto() {
        when(dpiRepository.findById(1)).thenReturn(Optional.empty());

        Optional<AssegnazioneDPI> result = assegnazioneDPIService.findById(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void eliminaAssegnazione_Esistente_EliminaELogga() {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        dpi.setTipo(AssegnazioneDPI.TipoDPI.ALTRO);
        Dipendente dipendente = new Dipendente();
        dipendente.setIdUtente(5);
        dpi.setDipendente(dipendente);

        when(dpiRepository.findById(1)).thenReturn(Optional.of(dpi));

        assegnazioneDPIService.eliminaAssegnazione(1);

        verify(dpiRepository).deleteById(1);
        verify(logService).registraEvento(
                eq("Aggiornamento registro DPI"),
                eq(true),
                anyString()
        );
    }

    @Test
    void eliminaAssegnazione_NonTrovato_LanciaEccezione() {
        when(dpiRepository.findById(1)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> assegnazioneDPIService.eliminaAssegnazione(1)
        );

        assertEquals("DPI non trovato con ID: 1", exception.getMessage());
        verify(dpiRepository, never()).deleteById(anyInt());
    }

    @Test
    void salvaAssegnazione_DataScadenzaPrimaDiConsegna_LanciaEccezione() {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        dpi.setDataConsegna(LocalDate.of(2025, 1, 10));
        dpi.setDataScadenzaRevisione(LocalDate.of(2025, 1, 5));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> assegnazioneDPIService.salvaAssegnazione(dpi)
        );

        assertEquals("La data di scadenza revisione deve essere successiva alla data di consegna.", exception.getMessage());
        verify(dpiRepository, never()).save(any());
    }

    @Test
    void salvaAssegnazione_NuovaAssegnazione_SalvaLoggaENotifica() {
        Dipendente dipendente = new Dipendente();
        dipendente.setIdUtente(10);

        AssegnazioneDPI dpiInput = new AssegnazioneDPI();
        dpiInput.setDipendente(dipendente);
        dpiInput.setTipo(AssegnazioneDPI.TipoDPI.ALTRO);
        dpiInput.setNomeDpi("Casco Speciale");

        AssegnazioneDPI dpiSalvato = new AssegnazioneDPI();
        dpiSalvato.setIdAssegnazione(1);
        dpiSalvato.setDipendente(dipendente);
        dpiSalvato.setTipo(AssegnazioneDPI.TipoDPI.ALTRO);
        dpiSalvato.setNomeDpi("Casco Speciale");
        dpiSalvato.setDataConsegna(LocalDate.now());

        when(dpiRepository.save(any(AssegnazioneDPI.class))).thenReturn(dpiSalvato);

        AssegnazioneDPI result = assegnazioneDPIService.salvaAssegnazione(dpiInput);

        assertNotNull(result.getDataConsegna());
        verify(dpiRepository).save(dpiInput);
        verify(logService).registraEvento(eq("Aggiornamento registro DPI"), eq(true), anyString());
        verify(notificaService, times(2)).inviaNotifica(eq(dipendente), anyString(), eq(Notifica.Priorita.ALTA), any());
    }

    @Test
    void salvaAssegnazione_AssegnazioneEsistente_SalvaENotificaMaNonLogga() {
        Dipendente dipendente = new Dipendente();
        dipendente.setIdUtente(10);

        AssegnazioneDPI dpiInput = new AssegnazioneDPI();
        dpiInput.setIdAssegnazione(1);
        dpiInput.setDipendente(dipendente);
        dpiInput.setTipo(AssegnazioneDPI.TipoDPI.ALTRO);
        dpiInput.setDataConsegna(LocalDate.now());

        when(dpiRepository.save(any(AssegnazioneDPI.class))).thenReturn(dpiInput);

        AssegnazioneDPI result = assegnazioneDPIService.salvaAssegnazione(dpiInput);

        assertEquals(1, result.getIdAssegnazione());
        verify(dpiRepository).save(dpiInput);
        verify(logService, never()).registraEvento(anyString(), anyBoolean(), anyString());
        verify(notificaService, times(2)).inviaNotifica(eq(dipendente), anyString(), eq(Notifica.Priorita.ALTA), any());
    }

    @Test
    void trovaPerDipendente_RitornaListaAssegnazioni() {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        when(dpiRepository.findByDipendenteIdUtente(5)).thenReturn(List.of(dpi));

        List<AssegnazioneDPI> result = assegnazioneDPIService.trovaPerDipendente(5);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void trovaInScadenzaRevisione_RitornaListaAssegnazioni() {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        when(dpiRepository.findByDataScadenzaRevisioneBefore(any(LocalDate.class))).thenReturn(List.of(dpi));

        List<AssegnazioneDPI> result = assegnazioneDPIService.trovaInScadenzaRevisione(30);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void convertToDTO_MappaTuttiICampiCorrettamente() {
        Dipendente dipendente = new Dipendente();
        dipendente.setIdUtente(8);

        AssegnazioneDPI dpi = new AssegnazioneDPI();
        dpi.setIdAssegnazione(100);
        dpi.setDipendente(dipendente);
        dpi.setTipo(AssegnazioneDPI.TipoDPI.ALTRO);
        dpi.setNomeDpi("Guanti");
        dpi.setDataConsegna(LocalDate.of(2023, 1, 1));
        dpi.setDataScadenzaRevisione(LocalDate.of(2023, 12, 31));

        AssegnazioneDPIDTO dto = assegnazioneDPIService.convertToDTO(dpi);

        assertEquals(100, dto.getIdAssegnazione());
        assertEquals(8, dto.getIdDipendente());
        assertEquals(AssegnazioneDPI.TipoDPI.ALTRO, dto.getTipo());
        assertEquals("Guanti", dto.getNomeDpi());
        assertEquals(LocalDate.of(2023, 1, 1), dto.getDataConsegna());
        assertEquals(LocalDate.of(2023, 12, 31), dto.getDataScadenzaRevisione());
        assertTrue(dto.isDaRevisionare());
    }

    @Test
    void convertToDTO_SenzaScadenzaRevisione_NonVaInErrore() {
        Dipendente dipendente = new Dipendente();
        dipendente.setIdUtente(8);

        AssegnazioneDPI dpi = new AssegnazioneDPI();
        dpi.setIdAssegnazione(100);
        dpi.setDipendente(dipendente);
        dpi.setTipo(AssegnazioneDPI.TipoDPI.ALTRO);

        dpi.setDataScadenzaRevisione(null);

        AssegnazioneDPIDTO dto = assegnazioneDPIService.convertToDTO(dpi);

        assertNull(dto.getDataScadenzaRevisione(), "La data di scadenza nel DTO deve essere null");

        assertFalse(dto.isDaRevisionare(), "Il flag daRevisionare deve essere false se non c'è una data");
    }
}
