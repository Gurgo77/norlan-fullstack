package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.AziendaDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.AziendaRepository;
import it.norlan.clientportal.repository.DipendenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di Business Logic del dominio B2B (Azienda).
 * Verifica la corretta orchestrazione dell'integrità referenziale (Cascade Deletes),
 * l'emissione condizionale degli eventi di Onboarding e l'aggregazione dei Data Transfer Object.
 */
@ExtendWith(MockitoExtension.class)
class AziendaServiceTest {

    @Mock
    private AziendaRepository aziendaRepository;

    @Mock
    private DipendenteRepository dipendenteRepository;

    @Mock
    private NotificaService notificaService;

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private AziendaService aziendaService;

    private Azienda azienda;

    @BeforeEach
    void setUp() {
        azienda = new Azienda();
        azienda.setIdUtente(1);
        azienda.setRagioneSociale("Norlan S.r.l.");
        azienda.setPartitaIva("12345678901");
    }

    @Test
    void findAll_RitornaListaAziende() {
        when(aziendaRepository.findAll()).thenReturn(List.of(azienda));

        List<Azienda> result = aziendaService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(aziendaRepository).findAll();
    }

    @Test
    void findById_Trovata_RitornaOptionalAzienda() {
        when(aziendaRepository.findById(1)).thenReturn(Optional.of(azienda));

        Optional<Azienda> result = aziendaService.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Norlan S.r.l.", result.get().getRagioneSociale());
    }

    // Event-Driven Lifecycle: verifica che il pattern Upsert distingua la creazione (ID nullo) dall'aggiornamento, innescando il workflow di Onboarding unicamente per le nuove entità
    @Test
    void salvaAzienda_NuovaAzienda_InviaNotificaBenvenuto() {
        Azienda nuovaAzienda = new Azienda();
        nuovaAzienda.setIdUtente(null);

        when(aziendaRepository.save(any(Azienda.class))).thenReturn(azienda);

        Azienda result = aziendaService.salvaAzienda(nuovaAzienda);

        assertNotNull(result);
        verify(notificaService).inviaNotifica(eq(azienda), anyString(), eq(Notifica.Priorita.MEDIA), eq(Notifica.CanaleNotifica.IN_APP));
        verify(aziendaRepository).save(nuovaAzienda);
    }

    @Test
    void salvaAzienda_AziendaEsistente_NonInviaNotifica() {
        when(aziendaRepository.save(any(Azienda.class))).thenReturn(azienda);

        Azienda result = aziendaService.salvaAzienda(azienda);

        assertNotNull(result);
        verifyNoInteractions(notificaService);
        verify(aziendaRepository).save(azienda);
    }

    // Integrità Referenziale Applicativa (Cascade Delete): assicura la rimozione a cascata delle entità figlie (Dipendenti) per prevenire record orfani, garantendo il tracciamento in Audit
    @Test
    void eliminaAzienda_Esistente_CancellaDipendentiELogga() {
        when(aziendaRepository.findById(1)).thenReturn(Optional.of(azienda));

        aziendaService.eliminaAzienda(1);

        verify(dipendenteRepository).deleteByAziendaId(1);
        verify(aziendaRepository).deleteById(1);
        verify(logService).registraEvento(eq("Eliminazione anagrafica: AZIENDA"), eq(true), contains("Norlan S.r.l."));
    }

    @Test
    void eliminaAzienda_NonTrovata_LanciaEccezione() {
        when(aziendaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> aziendaService.eliminaAzienda(99));
        verify(aziendaRepository, never()).deleteById(any());
    }

    // Data Aggregation Pattern: collauda la proiezione dei DTO verificando il calcolo a runtime dei campi derivati (Display Label) e l'interrogazione aggregata dello stato relazionale
    @Test
    void convertToDTO_MappaCampiCorrettamente() {
        azienda.setEmail("info@norlan.it");
        azienda.setRuolo(Utente.Ruolo.AZIENDA);

        when(dipendenteRepository.existsByAziendaIdUtente(1)).thenReturn(true);

        AziendaDTO dto = aziendaService.convertToDTO(azienda);

        assertEquals(azienda.getIdUtente(), dto.getIdUtente());
        assertEquals(azienda.getRagioneSociale(), dto.getRagioneSociale());
        assertEquals("Norlan S.r.l. (12345678901)", dto.getEtichettaDisplay());
        assertTrue(dto.isHasDipendenti());
        assertEquals(Utente.Ruolo.AZIENDA, dto.getRuolo());
    }
}
