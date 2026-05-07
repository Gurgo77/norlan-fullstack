package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.MaterialeDidatticoDTO;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.MaterialeDidatticoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialeDidatticoServiceTest {

    @Mock
    private MaterialeDidatticoRepository repository;

    @Mock
    private NotificaService notificaService;

    @Mock
    private IscrizioneCorsoRepository iscrizioneRepository;

    @InjectMocks
    private MaterialeDidatticoService materialeService;

    private MaterialeDidattico materiale;
    private CorsoFormazione corso;
    private Docente docente;

    @BeforeEach
    void setUp() {
        docente = new Docente();
        docente.setIdUtente(5);

        corso = new CorsoFormazione();
        corso.setIdCorso(10);
        corso.setTitolo("Corso Sicurezza Ambientale");
        corso.setDocente(docente);

        materiale = new MaterialeDidattico();
        materiale.setIdMateriale(1);
        materiale.setTitoloDocumento("Slide Lezione 1");
        materiale.setPercorsoFile("/docs/slide1.pdf");
        materiale.setCorso(corso);
    }

    @Test
    void trovaPerCorso_RitornaListaMateriali() {
        when(repository.findByCorsoIdCorso(10)).thenReturn(List.of(materiale));

        List<MaterialeDidattico> result = materialeService.trovaPerCorso(10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Slide Lezione 1", result.get(0).getTitoloDocumento());
    }

    @Test
    void findById_Trovato_RitornaOptionalPresente() {
        when(repository.findById(1)).thenReturn(Optional.of(materiale));

        Optional<MaterialeDidattico> result = materialeService.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getIdMateriale());
    }

    @Test
    void findById_NonTrovato_RitornaOptionalVuoto() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        Optional<MaterialeDidattico> result = materialeService.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void salvaMateriale_PercorsoFileNull_LanciaEccezione() {
        materiale.setPercorsoFile(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> materialeService.salvaMateriale(materiale)
        );

        assertEquals("Il percorso del file è obbligatorio per il materiale didattico.", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void salvaMateriale_PercorsoFileVuoto_LanciaEccezione() {
        materiale.setPercorsoFile("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> materialeService.salvaMateriale(materiale)
        );

        assertEquals("Il percorso del file è obbligatorio per il materiale didattico.", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void salvaMateriale_NuovoSenzaData_ImpostaDataESalvaENotifica() {
        materiale.setDataCaricamento(null);

        Dipendente studente = new Dipendente();
        studente.setIdUtente(20);
        IscrizioneCorso iscrizione = new IscrizioneCorso();
        iscrizione.setUtente(studente);

        when(repository.save(any(MaterialeDidattico.class))).thenReturn(materiale);
        when(iscrizioneRepository.findByCorsoIdCorso(10)).thenReturn(List.of(iscrizione));

        MaterialeDidattico result = materialeService.salvaMateriale(materiale);

        assertNotNull(result.getDataCaricamento());

        verify(repository).save(materiale);
        verify(notificaService).inviaNotifica(eq(docente), anyString(), eq(Notifica.Priorita.BASSA), eq(Notifica.CanaleNotifica.IN_APP));
        verify(notificaService).inviaNotifica(eq(studente), anyString(), eq(Notifica.Priorita.MEDIA), eq(Notifica.CanaleNotifica.IN_APP));
        verify(notificaService).inviaNotifica(eq(studente), anyString(), eq(Notifica.Priorita.MEDIA), eq(Notifica.CanaleNotifica.EMAIL));
    }

    @Test
    void salvaMateriale_NessunIscritto_NotificaSoloDocente() {
        when(repository.save(any(MaterialeDidattico.class))).thenReturn(materiale);
        when(iscrizioneRepository.findByCorsoIdCorso(10)).thenReturn(Collections.emptyList());

        materialeService.salvaMateriale(materiale);

        verify(notificaService, times(1)).inviaNotifica(any(), anyString(), any(), any());
        verify(notificaService).inviaNotifica(eq(docente), anyString(), eq(Notifica.Priorita.BASSA), eq(Notifica.CanaleNotifica.IN_APP));
    }

    @Test
    void salvaMateriale_SenzaDocente_NonVaInErrore() {
        corso.setDocente(null);
        when(repository.save(any(MaterialeDidattico.class))).thenReturn(materiale);
        when(iscrizioneRepository.findByCorsoIdCorso(10)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> materialeService.salvaMateriale(materiale));
        verify(notificaService, never()).inviaNotifica(any(), anyString(), any(), any());
    }

    @Test
    void eliminaMateriale_EsegueDelete() {
        materialeService.eliminaMateriale(1);

        verify(repository).deleteById(1);
    }

    @Test
    void convertToDTO_ConCorso_MappaTuttiICampi() {
        LocalDateTime ora = LocalDateTime.now();
        materiale.setDataCaricamento(ora);

        MaterialeDidatticoDTO dto = materialeService.convertToDTO(materiale);

        assertEquals(1, dto.getIdMateriale());
        assertEquals(10, dto.getIdCorso());
        assertEquals("Corso Sicurezza Ambientale", dto.getTitoloCorso());
        assertEquals("Slide Lezione 1", dto.getTitoloDocumento());
        assertEquals("/docs/slide1.pdf", dto.getPercorsoFile());
        assertEquals(ora, dto.getDataCaricamento());
    }

    @Test
    void convertToDTO_SenzaCorso_MappaSenzaErrori() {
        materiale.setCorso(null);

        MaterialeDidatticoDTO dto = materialeService.convertToDTO(materiale);

        assertEquals(1, dto.getIdMateriale());
        assertNull(dto.getIdCorso());
        assertNull(dto.getTitoloCorso());
        assertEquals("Slide Lezione 1", dto.getTitoloDocumento());
        assertEquals("/docs/slide1.pdf", dto.getPercorsoFile());
    }
}
