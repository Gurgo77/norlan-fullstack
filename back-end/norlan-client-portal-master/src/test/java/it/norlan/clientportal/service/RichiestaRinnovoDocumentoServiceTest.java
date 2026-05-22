package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.RichiestaRinnovoDocumentoDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import it.norlan.clientportal.repository.RichiestaRinnovoDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di orchestrazione dei Workflow Documentali (Rinnovi).
 * Verifica le policy di default fallback in fase di creazione (Defensive Initialization), la mutazione
 * guidata dagli eventi (Event-Driven State Mutation) e la Null-Safety nel mapping verso i DTO.
 */
@ExtendWith(MockitoExtension.class)
class RichiestaRinnovoDocumentoServiceTest {

    @Mock
    private RichiestaRinnovoDocumentoRepository repository;

    @Mock
    private NotificaService notificaService;

    @InjectMocks
    private RichiestaRinnovoDocumentoService richiestaService;

    private RichiestaRinnovoDocumento richiesta;
    private Documento documento;
    private Azienda azienda;

    @BeforeEach
    void setUp() {
        azienda = new Azienda();
        azienda.setIdUtente(1);
        azienda.setRagioneSociale("Norlan SPA");

        documento = new Documento();
        documento.setIdDocumento(10);
        documento.setTipologia(Documento.TipoDocumento.DVR);
        documento.setAzienda(azienda);

        richiesta = new RichiestaRinnovoDocumento();
        richiesta.setIdRichiesta(100);
        richiesta.setDocumento(documento);
        richiesta.setDataRichiesta(LocalDateTime.of(2026, 5, 6, 10, 0));
        richiesta.setStato(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA);
    }

    @Test
    void findAll_RitornaListaCompleta() {
        when(repository.findAll()).thenReturn(List.of(richiesta));

        List<RichiestaRinnovoDocumento> result = richiestaService.findAll();

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getIdRichiesta());
    }

    @Test
    void findById_RichiestaEsistente_RitornaOptionalPresente() {
        when(repository.findById(100)).thenReturn(Optional.of(richiesta));

        Optional<RichiestaRinnovoDocumento> result = richiestaService.findById(100);

        assertTrue(result.isPresent());
        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA, result.get().getStato());
    }

    @Test
    void findById_RichiestaNonEsistente_RitornaOptionalVuoto() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        Optional<RichiestaRinnovoDocumento> result = richiestaService.findById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void eliminaRichiesta_EsegueDeleteCorrettamente() {
        richiestaService.eliminaRichiesta(100);

        verify(repository).deleteById(100);
    }

    // Defensive Initialization (Default Fallback): assicura che il Service compensi eventuali payload parziali provenienti dai Controller, iniettando costanti di business (es. Stato IN_ATTESA) prima del flush sul DB
    @Test
    void creaRichiesta_ConDatiNulli_ImpostaValoriDiDefaultESalva() {
        RichiestaRinnovoDocumento nuovaRichiesta = new RichiestaRinnovoDocumento();
        nuovaRichiesta.setDataRichiesta(null);
        nuovaRichiesta.setStato(null);

        when(repository.save(any(RichiestaRinnovoDocumento.class))).thenAnswer(i -> i.getArgument(0));

        RichiestaRinnovoDocumento result = richiestaService.creaRichiesta(nuovaRichiesta);

        assertNotNull(result.getDataRichiesta());
        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA, result.getStato());
        verify(repository).save(nuovaRichiesta);
    }

    @Test
    void creaRichiesta_ConDatiValidi_MantieneValoriOriginaliESalva() {
        LocalDateTime dataSpecifica = LocalDateTime.of(2025, 1, 1, 12, 0);
        richiesta.setDataRichiesta(dataSpecifica);
        richiesta.setStato(RichiestaRinnovoDocumento.StatoRinnovo.IN_LAVORAZIONE);

        when(repository.save(any(RichiestaRinnovoDocumento.class))).thenReturn(richiesta);

        RichiestaRinnovoDocumento result = richiestaService.creaRichiesta(richiesta);

        assertEquals(dataSpecifica, result.getDataRichiesta());
        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_LAVORAZIONE, result.getStato());
        verify(repository).save(richiesta);
    }

    @Test
    void cambiaStato_RichiestaNonTrovata_NonEsegueSalvataggioENotifiche() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        richiestaService.cambiaStato(999, RichiestaRinnovoDocumento.StatoRinnovo.COMPLETATO);

        verify(repository, never()).save(any());
        verifyNoInteractions(notificaService);
    }

    @Test
    void cambiaStato_SenzaAzienda_AggiornaStatoSenzaInviareNotifiche() {
        richiesta.getDocumento().setAzienda(null);
        when(repository.findById(100)).thenReturn(Optional.of(richiesta));
        when(repository.save(any(RichiestaRinnovoDocumento.class))).thenReturn(richiesta);

        richiestaService.cambiaStato(100, RichiestaRinnovoDocumento.StatoRinnovo.COMPLETATO);

        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.COMPLETATO, richiesta.getStato());
        verify(repository).save(richiesta);
        verifyNoInteractions(notificaService);
    }

    // Event-Driven State Mutation: verifica che l'avanzamento della macchina a stati finiti (FSM) del workflow inneschi coerentemente il broadcasting multi-canale verso il tenant associato (Azienda)
    @Test
    void cambiaStato_ConAzienda_AggiornaStatoEInviaNotifiche() {
        when(repository.findById(100)).thenReturn(Optional.of(richiesta));
        when(repository.save(any(RichiestaRinnovoDocumento.class))).thenReturn(richiesta);

        richiestaService.cambiaStato(100, RichiestaRinnovoDocumento.StatoRinnovo.IN_LAVORAZIONE);

        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_LAVORAZIONE, richiesta.getStato());
        verify(repository).save(richiesta);

        verify(notificaService).inviaNotifica(
                eq(azienda),
                anyString(),
                eq(Notifica.Priorita.MEDIA),
                eq(Notifica.CanaleNotifica.IN_APP)
        );

        verify(notificaService).inviaNotifica(
                eq(azienda),
                anyString(),
                eq(Notifica.Priorita.MEDIA),
                eq(Notifica.CanaleNotifica.EMAIL)
        );
    }

    @Test
    void trovaPerStato_RitornaListaFiltrata() {
        when(repository.findByStato(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA))
                .thenReturn(List.of(richiesta));

        List<RichiestaRinnovoDocumento> result = richiestaService.trovaPerStato(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA);

        assertEquals(1, result.size());
        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA, result.get(0).getStato());
    }

    @Test
    void convertToDTO_RichiestaCompleta_MappaTuttiICampi() {
        RichiestaRinnovoDocumentoDTO dto = richiestaService.convertToDTO(richiesta);

        assertEquals(100, dto.getIdRichiesta());
        assertEquals(LocalDateTime.of(2026, 5, 6, 10, 0), dto.getDataRichiesta());
        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA, dto.getStato());
        assertEquals(10, dto.getIdDocumento());
        assertEquals("DVR", dto.getTipologiaDocumento());
        assertEquals("Norlan SPA", dto.getRagioneSocialeAzienda());
    }

    @Test
    void convertToDTO_RichiestaSenzaDocumento_MappaCampiBaseSenzaEccezioni() {
        richiesta.setDocumento(null);

        RichiestaRinnovoDocumentoDTO dto = richiestaService.convertToDTO(richiesta);

        assertEquals(100, dto.getIdRichiesta());
        assertEquals(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA, dto.getStato());
        assertNull(dto.getIdDocumento());
        assertNull(dto.getTipologiaDocumento());
        assertNull(dto.getRagioneSocialeAzienda());
    }
}
