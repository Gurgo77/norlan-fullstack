package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.IscrizioneCorsoDTO;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.model.IscrizioneCorso.IscrizioneId;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.UtenteRepository;
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
 * Suite di collaudo (Unit Test) per l'entità associativa IscrizioneCorso.
 * Verifica la resilienza delle logiche di associazione transazionale (Many-to-Many), l'idempotenza
 * degli inserimenti (prevenzione duplicati) e la complessa orchestrazione delle notifiche multi-attore (Event Broadcasting).
 */
@ExtendWith(MockitoExtension.class)
class IscrizioneCorsoServiceTest {

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private CorsoFormazioneRepository corsoRepository;

    @Mock
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Mock
    private NotificaService notificaService;

    @InjectMocks
    private IscrizioneCorsoService iscrizioneService;

    private IscrizioneCorso iscrizione;
    private Dipendente dipendente;
    private CorsoFormazione corso;
    private Docente docente;
    private Azienda azienda;
    private IscrizioneId iscrizioneId;

    @BeforeEach
    void setUp() {
        azienda = new Azienda();
        azienda.setIdUtente(2);
        azienda.setRagioneSociale("Test Srl");

        dipendente = new Dipendente();
        dipendente.setIdUtente(1);
        dipendente.setEmail("dipendente@test.it");
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setAzienda(azienda);

        docente = new Docente();
        docente.setIdUtente(3);
        docente.setEmail("docente@test.it");

        corso = new CorsoFormazione();
        corso.setIdCorso(10);
        corso.setTitolo("Corso Sicurezza");
        corso.setDataOrario(LocalDateTime.of(2026, 1, 1, 10, 0));
        corso.setLuogoFisico("Aula 1");
        corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);
        corso.setDocente(docente);

        iscrizioneId = new IscrizioneId(1, 10);

        iscrizione = new IscrizioneCorso();
        iscrizione.setId(iscrizioneId);
        iscrizione.setUtente(dipendente);
        iscrizione.setCorso(corso);
        iscrizione.setPresenzaConfermata(false);
    }

    @Test
    void findAll_RitornaLista() {
        when(iscrizioneRepository.findAll()).thenReturn(List.of(iscrizione));

        List<IscrizioneCorso> result = iscrizioneService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void trovaIscrizioniByCorso_RitornaLista() {
        when(iscrizioneRepository.findByCorsoIdCorso(10)).thenReturn(List.of(iscrizione));

        List<IscrizioneCorso> result = iscrizioneService.trovaIscrizioniByCorso(10);

        assertEquals(1, result.size());
    }

    @Test
    void existsById_Esiste_RitornaTrue() {
        when(iscrizioneRepository.existsById(iscrizioneId)).thenReturn(true);

        boolean result = iscrizioneService.existsById(1, 10);

        assertTrue(result);
    }

    @Test
    void eliminaIscrizione_EsegueDelete() {
        iscrizioneService.eliminaIscrizione(1, 10);

        verify(iscrizioneRepository).deleteById(iscrizioneId);
    }

    // Idempotency & Concurrency: collauda il meccanismo difensivo che impedisce iscrizioni ridondanti, preservando l'integrità referenziale della chiave primaria composita nel database
    @Test
    void iscriviUtente_GiaIscritto_LanciaEccezione() {
        when(iscrizioneRepository.existsById(iscrizioneId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> iscrizioneService.iscriviUtente(1, 10));
        verify(iscrizioneRepository, never()).save(any());
    }

    @Test
    void iscriviUtente_UtenteNonTrovato_LanciaEccezione() {
        when(iscrizioneRepository.existsById(iscrizioneId)).thenReturn(false);
        when(utenteRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> iscrizioneService.iscriviUtente(1, 10));
    }

    @Test
    void iscriviUtente_CorsoNonTrovato_LanciaEccezione() {
        when(iscrizioneRepository.existsById(iscrizioneId)).thenReturn(false);
        when(utenteRepository.findById(1)).thenReturn(Optional.of(dipendente));
        when(corsoRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> iscrizioneService.iscriviUtente(1, 10));
    }

    // Event Broadcasting Pattern: assicura che una transazione di business completata (iscrizione) propaghi correttamente l'evento asincrono ai vari stakeholder (Dipendente, Docente, Azienda)
    @Test
    void iscriviUtente_Successo_SalvaENotificaTutti() {
        when(iscrizioneRepository.existsById(iscrizioneId)).thenReturn(false);
        when(utenteRepository.findById(1)).thenReturn(Optional.of(dipendente));
        when(corsoRepository.findById(10)).thenReturn(Optional.of(corso));
        when(iscrizioneRepository.save(any(IscrizioneCorso.class))).thenAnswer(i -> i.getArgument(0));

        IscrizioneCorso result = iscrizioneService.iscriviUtente(1, 10);

        assertNotNull(result);
        assertEquals(iscrizioneId, result.getId());
        verify(iscrizioneRepository).save(any(IscrizioneCorso.class));
        verify(notificaService, times(2)).inviaNotifica(eq(dipendente), anyString(), any(), any());
        verify(notificaService, times(2)).inviaNotifica(eq(docente), anyString(), any(), any());
        verify(notificaService).inviaNotifica(eq(azienda), anyString(), eq(Notifica.Priorita.BASSA), eq(Notifica.CanaleNotifica.IN_APP));
    }

    @Test
    void validaPresenzaLavoratore_NonTrovata_LanciaEccezione() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> iscrizioneService.validaPresenzaLavoratore(10, 1));
    }

    @Test
    void validaPresenzaLavoratore_Successo_ImpostaTrueESalva() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        iscrizioneService.validaPresenzaLavoratore(10, 1);

        assertTrue(iscrizione.getPresenzaConfermata());
        verify(iscrizioneRepository).save(iscrizione);
    }

    @Test
    void getPathAttestato_IscrizioneNonTrovata_LanciaEccezione() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> iscrizioneService.getPathAttestato(1, 10));
    }

    @Test
    void getPathAttestato_NessunDocumento_RitornaNull() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        String path = iscrizioneService.getPathAttestato(1, 10);

        assertNull(path);
    }

    @Test
    void getPathAttestato_ConDocumento_RitornaPath() {
        Documento documento = new Documento();
        documento.setFilePath("/path/attestato.pdf");
        iscrizione.setDocumentoAttestato(documento);
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        String path = iscrizioneService.getPathAttestato(1, 10);

        assertEquals("/path/attestato.pdf", path);
    }

    @Test
    void trovaIscrizioniUtente_RitornaLista() {
        when(iscrizioneRepository.findByUtenteIdUtente(1)).thenReturn(List.of(iscrizione));

        List<IscrizioneCorso> result = iscrizioneService.trovaIscrizioniUtente(1);

        assertEquals(1, result.size());
    }

    @Test
    void convertToDTO_IscrizioneCompleta_MappaTuttiICampi() {
        Documento documento = new Documento();
        documento.setIdDocumento(99);
        iscrizione.setDocumentoAttestato(documento);

        IscrizioneCorsoDTO dto = iscrizioneService.convertToDTO(iscrizione);

        assertEquals(1, dto.getIdUtente());
        assertEquals(10, dto.getIdCorso());
        assertEquals("dipendente@test.it", dto.getEmailUtente());
        assertEquals("Corso Sicurezza", dto.getTitoloCorso());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), dto.getDataOrarioCorso());
        assertFalse(dto.getPresenzaConfermata());
        assertEquals(99, dto.getIdDocumento());
        assertEquals("PROGRAMMATO", dto.getStatoCorso());
    }

    @Test
    void convertToDTO_IscrizioneVuota_MappaSenzaErrori() {
        IscrizioneCorso iscrizioneVuota = new IscrizioneCorso();

        IscrizioneCorsoDTO dto = iscrizioneService.convertToDTO(iscrizioneVuota);

        assertNull(dto.getIdUtente());
        assertNull(dto.getIdCorso());
        assertNull(dto.getEmailUtente());
        assertNull(dto.getTitoloCorso());
        assertNull(dto.getDataOrarioCorso());
        assertFalse(dto.getPresenzaConfermata());
        assertNull(dto.getIdDocumento());
        assertNull(dto.getStatoCorso());
    }
}
