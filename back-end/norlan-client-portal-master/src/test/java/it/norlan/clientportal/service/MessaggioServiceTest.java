package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.MessaggioDTO;
import it.norlan.clientportal.model.Messaggio;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.repository.DipendenteRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import it.norlan.clientportal.repository.MessaggioRepository;
import it.norlan.clientportal.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessaggioServiceTest {

    @Mock
    private MessaggioRepository messaggioRepository;
    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private DipendenteRepository dipendenteRepository;
    @Mock
    private IscrizioneCorsoRepository iscrizioneCorsoRepository;
    @Mock
    private NotificaService notificaService;

    @InjectMocks
    private MessaggioService messaggioService;

    private Utente admin;
    private Utente azienda;
    private Utente dipendente;
    private Utente docente;
    private Messaggio messaggio;

    @BeforeEach
    void setUp() {
        admin = new Utente() {};
        admin.setIdUtente(1);
        admin.setRuolo(Utente.Ruolo.ADMIN);
        admin.setEmail("admin@test.it");

        azienda = new Utente() {};
        azienda.setIdUtente(2);
        azienda.setRuolo(Utente.Ruolo.AZIENDA);
        azienda.setEmail("azienda@test.it");

        dipendente = new Utente() {};
        dipendente.setIdUtente(3);
        dipendente.setRuolo(Utente.Ruolo.DIPENDENTE);
        dipendente.setEmail("dipendente@test.it");

        docente = new Utente() {};
        docente.setIdUtente(4);
        docente.setRuolo(Utente.Ruolo.DOCENTE);
        docente.setEmail("docente@test.it");

        messaggio = new Messaggio();
        messaggio.setIdMessaggio(100);
        messaggio.setMittente(dipendente);
        messaggio.setDestinatario(azienda);
        messaggio.setTesto("Richiesta informazioni");
        messaggio.setTimestampInvio(LocalDateTime.now());
        messaggio.setLetto(false);
    }

    @Test
    void salvaMessaggio_MittenteNonTrovato_LanciaEccezione() {
        when(utenteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> messaggioService.salvaMessaggio(99, 2, "Test"));
        verify(messaggioRepository, never()).save(any());
    }

    @Test
    void salvaMessaggio_DestinatarioNonTrovato_LanciaEccezione() {
        when(utenteRepository.findById(1)).thenReturn(Optional.of(admin));
        when(utenteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> messaggioService.salvaMessaggio(1, 99, "Test"));
        verify(messaggioRepository, never()).save(any());
    }

    @Test
    void salvaMessaggio_AdminVersoChiunque_Successo() {
        when(utenteRepository.findById(1)).thenReturn(Optional.of(admin));
        when(utenteRepository.findById(2)).thenReturn(Optional.of(azienda));
        when(messaggioRepository.save(any(Messaggio.class))).thenAnswer(i -> i.getArgument(0));

        Messaggio result = messaggioService.salvaMessaggio(1, 2, "Comunicazione di servizio");

        assertNotNull(result);
        assertEquals(admin, result.getMittente());
        verify(messaggioRepository).save(any(Messaggio.class));
        verify(notificaService, times(2)).inviaNotifica(eq(azienda), anyString(), any(), any());
    }

    @Test
    void salvaMessaggio_AziendaVersoProprioDipendente_Successo() {
        when(utenteRepository.findById(2)).thenReturn(Optional.of(azienda));
        when(utenteRepository.findById(3)).thenReturn(Optional.of(dipendente));
        when(dipendenteRepository.existsByIdUtenteAndAziendaIdUtente(3, 2)).thenReturn(true);
        when(messaggioRepository.save(any(Messaggio.class))).thenAnswer(i -> i.getArgument(0));

        Messaggio result = messaggioService.salvaMessaggio(2, 3, "Testo");

        assertNotNull(result);
        verify(messaggioRepository).save(any());
    }

    @Test
    void salvaMessaggio_AziendaVersoDipendenteEsterno_LanciaAccessDeniedException() {
        when(utenteRepository.findById(2)).thenReturn(Optional.of(azienda));
        when(utenteRepository.findById(3)).thenReturn(Optional.of(dipendente));
        when(dipendenteRepository.existsByIdUtenteAndAziendaIdUtente(3, 2)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> messaggioService.salvaMessaggio(2, 3, "Testo"));
    }

    @Test
    void salvaMessaggio_DipendenteVersoPropriaAzienda_Successo() {
        when(utenteRepository.findById(3)).thenReturn(Optional.of(dipendente));
        when(utenteRepository.findById(2)).thenReturn(Optional.of(azienda));
        when(dipendenteRepository.existsByIdUtenteAndAziendaIdUtente(3, 2)).thenReturn(true);
        when(messaggioRepository.save(any(Messaggio.class))).thenAnswer(i -> i.getArgument(0));

        Messaggio result = messaggioService.salvaMessaggio(3, 2, "Testo");

        assertNotNull(result);
        verify(messaggioRepository).save(any());
    }

    @Test
    void salvaMessaggio_DocenteVersoDipendenteIscritto_Successo() {
        when(utenteRepository.findById(4)).thenReturn(Optional.of(docente));
        when(utenteRepository.findById(3)).thenReturn(Optional.of(dipendente));
        when(iscrizioneCorsoRepository.isDipendenteIscrittoAlCorsoDelDocente(3, 4)).thenReturn(true);
        when(messaggioRepository.save(any(Messaggio.class))).thenAnswer(i -> i.getArgument(0));

        Messaggio result = messaggioService.salvaMessaggio(4, 3, "Testo");

        assertNotNull(result);
        verify(messaggioRepository).save(any());
    }

    @Test
    void salvaMessaggio_DipendenteVersoDocenteNonIscritto_LanciaAccessDeniedException() {
        when(utenteRepository.findById(3)).thenReturn(Optional.of(dipendente));
        when(utenteRepository.findById(4)).thenReturn(Optional.of(docente));
        when(iscrizioneCorsoRepository.isDipendenteIscrittoAlCorsoDelDocente(3, 4)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> messaggioService.salvaMessaggio(3, 4, "Testo"));
    }

    @Test
    void salvaMessaggio_ComunicazioneNonGestita_LanciaAccessDeniedException() {
        // Test di comunicazione tra Azienda e Docente (non prevista dalle regole di business)
        when(utenteRepository.findById(2)).thenReturn(Optional.of(azienda));
        when(utenteRepository.findById(4)).thenReturn(Optional.of(docente));

        assertThrows(AccessDeniedException.class, () -> messaggioService.salvaMessaggio(2, 4, "Testo"));
    }

    @Test
    void salvaMessaggio_Successo_VerificaInvioNotificheCorrette() {
        when(utenteRepository.findById(1)).thenReturn(Optional.of(admin));
        when(utenteRepository.findById(3)).thenReturn(Optional.of(dipendente));
        when(messaggioRepository.save(any(Messaggio.class))).thenReturn(messaggio);

        messaggioService.salvaMessaggio(1, 3, "Testo Messaggio");

        verify(notificaService).inviaNotifica(
                eq(dipendente),
                eq("Hai ricevuto un nuovo messaggio da admin@test.it"),
                eq(Notifica.Priorita.MEDIA),
                eq(Notifica.CanaleNotifica.IN_APP)
        );

        verify(notificaService).inviaNotifica(
                eq(dipendente),
                eq("[CHAT]admin@test.it|Testo Messaggio"),
                eq(Notifica.Priorita.ALTA),
                eq(Notifica.CanaleNotifica.EMAIL)
        );
    }

    @Test
    void getCronologia_RitornaListaOrdinataEConvertita() {
        when(messaggioRepository.findConversazione(3, 2)).thenReturn(List.of(messaggio));

        List<MessaggioDTO> result = messaggioService.getCronologia(3, 2);

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getIdMessaggio());
        assertEquals("Richiesta informazioni", result.get(0).getTesto());
    }

    @Test
    void convertToDTO_MappaTuttiICampiInModoCompleto() {
        MessaggioDTO dto = messaggioService.convertToDTO(messaggio);

        assertEquals(100, dto.getIdMessaggio());
        assertEquals(3, dto.getIdMittente());
        assertEquals("dipendente@test.it", dto.getNomeMittente());
        assertEquals(2, dto.getIdDestinatario());
        assertEquals("Richiesta informazioni", dto.getTesto());
        assertNotNull(dto.getTimestampInvio());
        assertFalse(dto.getLetto());
    }
}
