package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.NotificaDTO;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.NotificaRepository;
import it.norlan.clientportal.strategy.NotificaContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di orchestrazione delle Notifiche.
 * Verifica l'integrazione del Behavioral Design Pattern (Strategy) per il routing
 * multi-canale e la corretta mutazione di stato (Lifecycle) dei messaggi asincroni.
 */
@ExtendWith(MockitoExtension.class)
class NotificaServiceTest {

    @Mock
    private NotificaContext notificaContext;

    @Mock
    private NotificaRepository notificaRepository;

    @InjectMocks
    private NotificaService notificaService;

    private Notifica notifica;
    private Utente destinatario;

    @BeforeEach
    void setUp() {
        destinatario = new Utente() {};
        destinatario.setIdUtente(1);
        destinatario.setEmail("utente@test.it");

        notifica = new Notifica();
        notifica.setIdNotifica(100);
        notifica.setDestinatario(destinatario);
        notifica.setMessaggio("Messaggio di test");
        notifica.setPriorita(Notifica.Priorita.ALTA);
        notifica.setCanale(Notifica.CanaleNotifica.IN_APP);
        notifica.setDataInvio(LocalDateTime.now());
        notifica.setLetta(false);
    }

    @Test
    void findById_Trovata_RitornaOptionalPresente() {
        when(notificaRepository.findById(100)).thenReturn(Optional.of(notifica));

        Optional<Notifica> result = notificaService.findById(100);

        assertTrue(result.isPresent());
        assertEquals("Messaggio di test", result.get().getMessaggio());
    }

    @Test
    void findById_NonTrovata_RitornaOptionalVuoto() {
        when(notificaRepository.findById(999)).thenReturn(Optional.empty());

        Optional<Notifica> result = notificaService.findById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void eliminaNotifica_EsegueDelete() {
        notificaService.eliminaNotifica(100);

        verify(notificaRepository).deleteById(100);
    }

    @Test
    void getNotificheNonLette_RitornaLista() {
        when(notificaRepository.findByDestinatarioIdUtenteAndLettaFalseOrderByDataInvioDesc(1))
                .thenReturn(List.of(notifica));

        List<Notifica> result = notificaService.getNotificheNonLette(1);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getLetta());
    }

    // Strategy Pattern & Payload Interception: verifica che il Service assembli l'entità (Timestamp, Default Flags) e deleghi dinamicamente il delivery al Context designato, validandone l'integrità tramite ArgumentCaptor
    @Test
    void inviaNotifica_CreaNotificaEEsegueStrategia() {
        notificaService.inviaNotifica(
                destinatario,
                "Nuovo avviso",
                Notifica.Priorita.MEDIA,
                Notifica.CanaleNotifica.EMAIL
        );

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(notificaContext).eseguiStrategia(captor.capture());

        Notifica captured = captor.getValue();
        assertEquals(destinatario, captured.getDestinatario());
        assertEquals("Nuovo avviso", captured.getMessaggio());
        assertEquals(Notifica.Priorita.MEDIA, captured.getPriorita());
        assertEquals(Notifica.CanaleNotifica.EMAIL, captured.getCanale());
        assertFalse(captured.getLetta());
        assertNotNull(captured.getDataInvio());
    }

    @Test
    void segnaComeLetta_NotificaTrovata_AggiornaESalva() {
        when(notificaRepository.findById(100)).thenReturn(Optional.of(notifica));

        notificaService.segnaComeLetta(100);

        assertTrue(notifica.getLetta());
        verify(notificaRepository).save(notifica);
    }

    // Graceful Error Handling (Idempotenza): garantisce che il tentativo di mutazione di una risorsa inesistente non scateni eccezioni bloccanti, preservando la stabilità del flusso esecutivo
    @Test
    void segnaComeLetta_NotificaNonTrovata_NessunSalvataggio() {
        when(notificaRepository.findById(999)).thenReturn(Optional.empty());

        notificaService.segnaComeLetta(999);

        verify(notificaRepository, never()).save(any(Notifica.class));
    }

    @Test
    void getNotificheUtente_RitornaLista() {
        when(notificaRepository.findByDestinatarioIdUtenteOrderByDataInvioDesc(1))
                .thenReturn(List.of(notifica));

        List<Notifica> result = notificaService.getNotificheUtente(1);

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getIdNotifica());
    }

    @Test
    void contaNonLette_RitornaConteggio() {
        when(notificaRepository.countByDestinatarioIdUtenteAndLettaFalse(1)).thenReturn(5L);

        long result = notificaService.contaNonLette(1);

        assertEquals(5L, result);
    }

    @Test
    void convertToDTO_NotificaCompleta_MappaTuttiICampi() {
        NotificaDTO dto = notificaService.convertToDTO(notifica);

        assertEquals(100, dto.getIdNotifica());
        assertEquals(1, dto.getIdDestinatario());
        assertEquals("utente@test.it", dto.getEmailDestinatario());
        assertEquals("Messaggio di test", dto.getMessaggio());
        assertFalse(dto.getLetta());
        assertEquals(Notifica.Priorita.ALTA, dto.getPriorita());
        assertNotNull(dto.getDataInvio());
    }

    // Defensive Mapping (Null-Safety): collauda la resilienza del Data Transfer Object, assicurando che la serializzazione non provochi NullPointerException in presenza di relazioni orfane
    @Test
    void convertToDTO_SenzaDestinatario_MappaSenzaErrori() {
        notifica.setDestinatario(null);

        NotificaDTO dto = notificaService.convertToDTO(notifica);

        assertEquals(100, dto.getIdNotifica());
        assertNull(dto.getIdDestinatario());
        assertNull(dto.getEmailDestinatario());
        assertEquals("Messaggio di test", dto.getMessaggio());
        assertFalse(dto.getLetta());
        assertEquals(Notifica.Priorita.ALTA, dto.getPriorita());
    }
}
