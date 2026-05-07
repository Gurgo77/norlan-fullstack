package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.CorsoFormazioneDTO;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.repository.CorsoFormazioneRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorsoFormazioneServiceTest {

    @Mock
    private CorsoFormazioneRepository corsoRepository;
    @Mock
    private IscrizioneCorsoRepository iscrizioneRepository;
    @Mock
    private MaterialeDidatticoRepository materialeRepository;
    @Mock
    private NotificaService notificaService;
    @Mock
    private LogSincronizzazioneService logService;
    @Mock
    private AdminService adminService;

    @InjectMocks
    private CorsoFormazioneService corsoService;

    private CorsoFormazione corso;
    private Docente docente;

    @BeforeEach
    void setUp() {
        docente = new Docente();
        docente.setIdUtente(10);
        docente.setEmail("docente@test.it");

        corso = new CorsoFormazione();
        corso.setIdCorso(1);
        corso.setTitolo("Corso Sicurezza Base");
        corso.setDocente(docente);
        corso.setDataOrario(LocalDateTime.now().plusDays(5));
        corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);
    }

    @Test
    void findAll_RitornaListaCompleta() {
        when(corsoRepository.findAll()).thenReturn(List.of(corso));

        List<CorsoFormazione> result = corsoService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void findById_CorsoEsistente_RitornaOptionalPresente() {
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));

        Optional<CorsoFormazione> result = corsoService.findById(1);

        assertTrue(result.isPresent());
    }

    @Test
    void trovaCorsiPerStato_FiltraCorrettamente() {
        when(corsoRepository.findByStato(CorsoFormazione.StatoCorso.PROGRAMMATO)).thenReturn(List.of(corso));

        List<CorsoFormazione> result = corsoService.trovaCorsiPerStato(CorsoFormazione.StatoCorso.PROGRAMMATO);

        assertFalse(result.isEmpty());
    }

    @Test
    void salvaCorso_DataPassata_LanciaIllegalArgumentException() {
        corso.setDataOrario(LocalDateTime.now().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> corsoService.salvaCorso(corso));
    }

    @Test
    void salvaCorso_NuovoCorso_ImpostaStatoLoggaENotifica() {
        corso.setIdCorso(null);
        corso.setStato(null);
        when(corsoRepository.save(any(CorsoFormazione.class))).thenReturn(corso);

        corsoService.salvaCorso(corso);

        assertEquals(CorsoFormazione.StatoCorso.PROGRAMMATO, corso.getStato());
        verify(logService).registraEvento(anyString(), eq(true), anyString());
        verify(notificaService, times(2)).inviaNotifica(eq(docente), anyString(), any(), any());
    }

    @Test
    void eliminaCorso_RimuoveMaterialiEIscrizioni() {
        when(materialeRepository.findByCorsoIdCorso(1)).thenReturn(List.of(new MaterialeDidattico()));
        when(iscrizioneRepository.findByCorsoIdCorso(1)).thenReturn(List.of(new IscrizioneCorso()));

        corsoService.eliminaCorso(1);

        verify(materialeRepository).deleteAll(anyList());
        verify(iscrizioneRepository).deleteAll(anyList());
        verify(corsoRepository).deleteById(1);
    }

    @Test
    void aggiornaStato_CorsoNonTrovato_LanciaIllegalArgumentException() {
        when(corsoRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> corsoService.aggiornaStato(1, CorsoFormazione.StatoCorso.IN_SVOLGIMENTO));
    }

    @Test
    void aggiornaStato_InSvolgimentoSenzaIscritti_LanciaIllegalStateException() {
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));
        when(iscrizioneRepository.findByCorsoIdCorso(1)).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class,
                () -> corsoService.aggiornaStato(1, CorsoFormazione.StatoCorso.IN_SVOLGIMENTO));
    }

    @Test
    void aggiornaStato_InSvolgimentoConIscritti_AggiornaENotifica() {
        IscrizioneCorso isc = new IscrizioneCorso();
        Dipendente studente = new Dipendente();
        isc.setUtente(studente);

        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));
        when(iscrizioneRepository.findByCorsoIdCorso(1)).thenReturn(List.of(isc));
        when(corsoRepository.save(any())).thenReturn(corso);

        corsoService.aggiornaStato(1, CorsoFormazione.StatoCorso.IN_SVOLGIMENTO);

        assertEquals(CorsoFormazione.StatoCorso.IN_SVOLGIMENTO, corso.getStato());
        verify(notificaService, times(2)).inviaNotifica(eq(docente), anyString(), any(), any());
        verify(notificaService).inviaNotifica(eq(studente), anyString(), any(), eq(Notifica.CanaleNotifica.EMAIL));
    }

    @Test
    void aggiornaStato_AltroStato_NotificaSoloDocente() {
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));
        when(corsoRepository.save(any())).thenReturn(corso);

        corsoService.aggiornaStato(1, CorsoFormazione.StatoCorso.CONCLUSO);

        assertEquals(CorsoFormazione.StatoCorso.CONCLUSO, corso.getStato());
        verify(notificaService, times(2)).inviaNotifica(eq(docente), anyString(), any(), any());
        verify(iscrizioneRepository, never()).findByCorsoIdCorso(anyInt());
    }

    @Test
    void concludiCorso_NotificaPartecipantiEAdmin() {
        IscrizioneCorso partecipante = new IscrizioneCorso();
        partecipante.setUtente(new Dipendente());
        Admin admin = new Admin();

        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));
        when(iscrizioneRepository.findByCorsoAndPresenzaConfermataTrue(corso)).thenReturn(List.of(partecipante));
        when(adminService.getUnicoAdmin()).thenReturn(Optional.of(admin));

        corsoService.concludiCorso(1);

        assertEquals(CorsoFormazione.StatoCorso.CONCLUSO, corso.getStato());
        verify(notificaService, times(2)).inviaNotifica(eq(partecipante.getUtente()), anyString(), any(), any());
        verify(notificaService, times(2)).inviaNotifica(eq(admin), anyString(), any(), any());
    }

    @Test
    void validaPresenzeAdmin_CorsoNonConcluso_LanciaIllegalStateException() {
        corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));

        assertThrows(IllegalStateException.class, () -> corsoService.validaPresenzeAdmin(1, List.of(5)));
    }

    @Test
    void validaPresenzeAdmin_Successo_ValidaENonValidaCorrettamente() {
        corso.setStato(CorsoFormazione.StatoCorso.CONCLUSO);

        Dipendente d1 = new Dipendente(); d1.setIdUtente(5);
        IscrizioneCorso i1 = new IscrizioneCorso(); i1.setUtente(d1);

        Dipendente d2 = new Dipendente(); d2.setIdUtente(8);
        IscrizioneCorso i2 = new IscrizioneCorso(); i2.setUtente(d2);

        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));
        when(iscrizioneRepository.findByCorsoIdCorso(1)).thenReturn(List.of(i1, i2));

        corsoService.validaPresenzeAdmin(1, List.of(5));

        assertTrue(i1.getPresenzaConfermata());
        assertFalse(i2.getPresenzaConfermata());
        assertEquals(CorsoFormazione.StatoCorso.ATTESA_FIRMA_DOCENTE, corso.getStato());
        verify(iscrizioneRepository).saveAll(anyList());
        verify(logService).registraEvento(eq("Validazione registro didattico"), eq(true), anyString());
    }

    @Test
    void controfirmaDocente_DocenteNonTitolare_LanciaSecurityException() {
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));

        assertThrows(SecurityException.class, () -> corsoService.controfirmaDocente(1, 999));
    }

    @Test
    void controfirmaDocente_StatoErrato_LanciaIllegalStateException() {
        corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));

        assertThrows(IllegalStateException.class, () -> corsoService.controfirmaDocente(1, 10));
    }

    @Test
    void controfirmaDocente_Successo_ImpostaValidato() {
        corso.setStato(CorsoFormazione.StatoCorso.ATTESA_FIRMA_DOCENTE);
        when(corsoRepository.findById(1)).thenReturn(Optional.of(corso));

        corsoService.controfirmaDocente(1, 10);

        assertEquals(CorsoFormazione.StatoCorso.VALIDATO, corso.getStato());
        verify(corsoRepository).save(corso);
    }

    @Test
    void convertToDTO_SenzaDocente_MappaCorrettamente() {
        corso.setDocente(null);

        CorsoFormazioneDTO dto = corsoService.convertToDTO(corso);

        assertEquals(corso.getIdCorso(), dto.getIdCorso());
        assertEquals(corso.getTitolo(), dto.getTitolo());
        assertNull(dto.getIdDocente());
        assertNull(dto.getEmailDocente());
    }

    @Test
    void convertToDTO_ConDocente_MappaCorrettamente() {
        CorsoFormazioneDTO dto = corsoService.convertToDTO(corso);

        assertEquals(corso.getIdCorso(), dto.getIdCorso());
        assertEquals(corso.getTitolo(), dto.getTitolo());
        assertEquals(10, dto.getIdDocente());
        assertEquals("docente@test.it", dto.getEmailDocente());
    }
}
