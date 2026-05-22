package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.DocumentoDTO;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.repository.DocumentoRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.state.documento.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per l'orchestrazione del Workflow Documentale.
 * Verifica l'integrazione del Behavioral Pattern (State), la protezione temporale contro caricamenti
 * futili (Guard Clauses cronologiche) e l'esecuzione sicura di elaborazioni massive (Batch Processing degli attestati).
 */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private CorsoFormazioneRepository corsoRepository;
    @Mock
    private IscrizioneCorsoRepository iscrizioneRepository;
    @Mock
    private DocumentoRepository documentoRepository;
    @Mock
    private NotificaService notificaService;
    @Mock
    private LogSincronizzazioneService logService;
    @Mock
    private AdminService adminService;

    @InjectMocks
    private DocumentoService documentoService;

    private Documento documento;
    private Azienda azienda;

    @BeforeEach
    void setUp() {

        azienda = new Azienda();
        azienda.setIdUtente(1);
        azienda.setRagioneSociale("Azienda Test Srl");
        documento = new Documento();
        documento.setIdDocumento(100);
        documento.setAzienda(azienda);
        documento.setTipologia(Documento.TipoDocumento.DVR);
        documento.setModulo(Documento.ModuloServizio.SICUREZZA);
        documento.setStato(new StatoCaricato());
        documento.setFilePath("/docs/dvr_2026.pdf");
        documento.setDataCaricamento(LocalDate.now());
        documento.setDataScadenza(LocalDate.now().plusYears(1));
    }

    @Test
    void findAll_RitornaListaCompleta() {
        when(documentoRepository.findAll()).thenReturn(List.of(documento));

        List<Documento> result = documentoService.findAll();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void findById_RitornaOptionalCorretto() {
        when(documentoRepository.findById(100)).thenReturn(Optional.of(documento));

        Optional<Documento> result = documentoService.findById(100);

        assertTrue(result.isPresent());
        assertEquals(Documento.TipoDocumento.DVR, result.get().getTipologia());
    }

    @Test
    void findByAzienda_RitornaListaDocumenti() {
        when(documentoRepository.findByAziendaIdUtente(1)).thenReturn(List.of(documento));

        List<Documento> result = documentoService.findByAzienda(1);

        assertEquals(1, result.size());
    }

    @Test
    void salvaDocumento_DataScadenzaNull_LanciaIllegalArgumentException() {
        documento.setDataScadenza(null);

        assertThrows(IllegalArgumentException.class, () -> documentoService.salvaDocumento(documento));
        verify(documentoRepository, never()).save(any());
    }

    // Quality Gate Temporale (Anti-Corruption Layer): assicura che il sistema rigetti preventivamente documenti con ciclo di vita residuo insufficiente, tutelando l'affidabilità del monitoraggio KPI
    @Test
    void salvaDocumento_DataScadenzaTroppoVicina_LanciaIllegalArgumentException() {
        documento.setDataScadenza(LocalDate.now().plusDays(10));

        assertThrows(IllegalArgumentException.class, () -> documentoService.salvaDocumento(documento));
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void salvaDocumento_DatiValidi_SalvaLoggaENotifica() {
        when(documentoRepository.save(any(Documento.class))).thenReturn(documento);

        Documento result = documentoService.salvaDocumento(documento);

        assertEquals(100, result.getIdDocumento());
        verify(logService).registraEvento(eq("Upload nuovo documento aziendale"), eq(true), anyString());
        verify(notificaService, times(2)).inviaNotifica(eq(azienda), anyString(), eq(Notifica.Priorita.MEDIA), any());
    }

    @Test
    void eliminaDocumento_DocNonTrovato_LanciaEccezione() {
        when(documentoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> documentoService.eliminaDocumento(999));
        verify(documentoRepository, never()).deleteById(anyInt());
    }

    @Test
    void eliminaDocumento_Successo_EliminaELogga() {
        when(documentoRepository.findById(100)).thenReturn(Optional.of(documento));

        documentoService.eliminaDocumento(100);

        verify(documentoRepository).deleteById(100);
        verify(logService).registraEvento(eq("Eliminazione documento di sistema"), eq(true), anyString());
    }

    @Test
    void trovaDocumentiInScadenza_RitornaListaFiltrata() {
        when(documentoRepository.findByDataScadenzaBefore(any(LocalDate.class))).thenReturn(List.of(documento));

        List<Documento> result = documentoService.trovaDocumentiInScadenza(30);

        assertEquals(1, result.size());
    }

    @Test
    void isScaduto_Scaduto_RitornaTrue() {
        documento.setDataScadenza(LocalDate.now().minusDays(5));
        assertTrue(documentoService.isScaduto(documento));
    }

    @Test
    void isScaduto_NonScaduto_RitornaFalse() {
        assertFalse(documentoService.isScaduto(documento));
    }

    @Test
    void isScaduto_DataScadenzaNull_RitornaFalse() {
        documento.setDataScadenza(null);
        assertFalse(documentoService.isScaduto(documento));
    }

    @Test
    void richiediFirmaDocumento_DocNonTrovato_LanciaEccezione() {
        when(documentoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> documentoService.richiediFirmaDocumento(999));
    }

    @Test
    void richiediFirmaDocumento_Successo_AggiornaStatoENotifica() {
        when(documentoRepository.findById(100)).thenReturn(Optional.of(documento));

        documentoService.richiediFirmaDocumento(100);

        assertInstanceOf(StatoInAttesaFirma.class, documento.getStato());
        verify(documentoRepository).save(documento);
        verify(notificaService, times(2)).inviaNotifica(eq(azienda), anyString(), eq(Notifica.Priorita.ALTA), any());
    }

    @Test
    void approvaDocumento_DocGenerico_ApprovaESalvaSenzaAltreNotifiche() {
        documento.setStato(new StatoInAttesaFirma());
        when(documentoRepository.findById(100)).thenReturn(Optional.of(documento));

        documentoService.approvaDocumento(100);

        assertInstanceOf(StatoApprovato.class, documento.getStato());
        verify(documentoRepository).save(documento);
        verifyNoInteractions(iscrizioneRepository);
    }

    // State-Driven Event Routing: collauda la transizione di stato verso "Approvato", garantendo che, in base al payload (Tipo Documento), venga innescato un routing condizionale delle notifiche
    @Test
    void approvaDocumento_AttestatoCorso_NotificaUtentiEAdmin() {
        documento.setStato(new StatoInAttesaFirma());
        documento.setTipologia(Documento.TipoDocumento.ATTESTATO_CORSO);

        IscrizioneCorso isc = new IscrizioneCorso();
        Dipendente d = new Dipendente();
        CorsoFormazione c = new CorsoFormazione();
        c.setTitolo("Corso Antincendio");
        isc.setUtente(d);
        isc.setCorso(c);

        when(documentoRepository.findById(100)).thenReturn(Optional.of(documento));
        when(iscrizioneRepository.findByDocumentoAttestatoIdDocumento(100)).thenReturn(List.of(isc));
        when(adminService.getUnicoAdmin()).thenReturn(Optional.of(new Admin()));

        documentoService.approvaDocumento(100);

        verify(notificaService, times(2)).inviaNotifica(eq(d), anyString(), eq(Notifica.Priorita.ALTA), any());
        verify(notificaService, times(2)).inviaNotifica(any(Admin.class), anyString(), eq(Notifica.Priorita.MEDIA), any());
    }

    @Test
    void archiviaDocumento_Successo_ImpostaStatoArchiviato() {
        documento.setStato(new StatoApprovato());
        when(documentoRepository.findById(100)).thenReturn(Optional.of(documento));

        documentoService.archiviaDocumento(100);

        assertInstanceOf(StatoArchiviato.class, documento.getStato());
        verify(documentoRepository).save(documento);
    }

    @Test
    void distribuisciAttestatiMassivi_CorsoNonTrovato_LanciaEccezione() {
        when(corsoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> documentoService.distribuisciAttestatiMassivi(999, new HashMap<>()));
    }

    @Test
    void distribuisciAttestatiMassivi_StatoErrato_LanciaEccezione() {
        CorsoFormazione corso = new CorsoFormazione();
        corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);

        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));

        assertThrows(IllegalStateException.class, () -> documentoService.distribuisciAttestatiMassivi(1, new HashMap<>()));
    }

    // Elaborazione Batch Relazionale: verifica l'orchestrazione di un'operazione massiva complessa, assicurando il corretto binding tra gli artefatti binari (PDF), le entità figlie (Iscrizioni) e il workflow padre (Corso)
    @Test
    void distribuisciAttestatiMassivi_Successo_GeneraAttestatiEAggiornaCorso() {
        CorsoFormazione corso = new CorsoFormazione();
        corso.setStato(CorsoFormazione.StatoCorso.VALIDATO);

        Dipendente d = new Dipendente();
        d.setAzienda(azienda);

        IscrizioneCorso isc = new IscrizioneCorso();
        isc.setPresenzaConfermata(true);
        isc.setUtente(d);

        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));
        when(iscrizioneRepository.findByCorsoIdCorso(1)).thenReturn(List.of(isc));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Map<Integer, String> paths = new HashMap<>();
        paths.put(1, "/attestati/azienda1.pdf");

        documentoService.distribuisciAttestatiMassivi(1, paths);

        assertEquals(CorsoFormazione.StatoCorso.CERTIFICATO, corso.getStato());
        verify(corsoRepository).save(corso);
        verify(documentoRepository, atLeastOnce()).save(any(Documento.class));
        verify(iscrizioneRepository).saveAll(anyList());
        verify(notificaService).inviaNotifica(eq(azienda), anyString(), eq(Notifica.Priorita.ALTA), eq(Notifica.CanaleNotifica.IN_APP));
        verify(logService).registraEvento(eq("Distribuzione attestati formativi"), eq(true), anyString());
    }

    @Test
    void convertToDTO_ConDataScadenzaValida_MappaCorrettamente() {
        DocumentoDTO dto = documentoService.convertToDTO(documento);

        assertEquals(100, dto.getIdDocumento());
        assertEquals(1, dto.getIdAzienda());
        assertEquals("Azienda Test Srl", dto.getRagioneSocialeAzienda());
        assertEquals(Documento.ModuloServizio.SICUREZZA, dto.getModulo());
        assertEquals(Documento.TipoDocumento.DVR, dto.getTipologia());
        assertEquals("CARICATO", dto.getStato());
        assertFalse(dto.isScaduto());
    }

    @Test
    void convertToDTO_SenzaDataScadenza_NonVaInErrore() {
        documento.setDataScadenza(null);

        DocumentoDTO dto = documentoService.convertToDTO(documento);

        assertFalse(dto.isScaduto());
    }
}
