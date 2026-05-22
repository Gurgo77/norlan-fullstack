package it.norlan.clientportal.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.norlan.clientportal.dto.LogSincronizzazioneDTO;
import it.norlan.clientportal.dto.NotificaDTO;
import it.norlan.clientportal.model.LogSincronizzazione;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import it.norlan.clientportal.service.NotificaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Suite di collaudo (Unit Test) per il layer REST di amministrazione e telemetria di sistema.
 * Verifica l'esposizione delle metriche di Auditing (Log di Sincronizzazione) e il ciclo di vita
 * delle notifiche, collaudando le procedure di Housekeeping (manutenzione e pulizia dati obsoleti).
 */
@ExtendWith(MockitoExtension.class)
class SistemaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private NotificaService notificaService;

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private SistemaController sistemaController;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(sistemaController).build();
    }

    @Test
    void getNotificheUtente_RitornaLista200() throws Exception {
        Notifica notifica = new Notifica();
        NotificaDTO dto = new NotificaDTO();
        dto.setIdNotifica(1);

        when(notificaService.getNotificheUtente(10)).thenReturn(List.of(notifica));
        when(notificaService.convertToDTO(notifica)).thenReturn(dto);

        mockMvc.perform(get("/api/sistema/notifiche/utente/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idNotifica").value(1));
    }

    @Test
    void getNotificheNonLette_RitornaLista200() throws Exception {
        Notifica notifica = new Notifica();
        NotificaDTO dto = new NotificaDTO();
        dto.setIdNotifica(2);

        when(notificaService.getNotificheNonLette(10)).thenReturn(List.of(notifica));
        when(notificaService.convertToDTO(notifica)).thenReturn(dto);

        mockMvc.perform(get("/api/sistema/notifiche/utente/10/non-lette"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idNotifica").value(2));
    }

    @Test
    void contaNotificheNonLette_RitornaConteggio200() throws Exception {
        when(notificaService.contaNonLette(10)).thenReturn(5L);

        mockMvc.perform(get("/api/sistema/notifiche/utente/10/non-lette/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
    // Semantic HTTP Verbs: valida l'uso del verbo PATCH per le mutazioni di stato parziali (es. cambio flag "letta"), ottimizzando il traffico di rete rispetto a una PUT completa
    @Test
    void segnaNotificaComeLetta_Ritorna200() throws Exception {
        mockMvc.perform(patch("/api/sistema/notifiche/1/letta"))
                .andExpect(status().isOk());

        verify(notificaService).segnaComeLetta(1);
    }

    @Test
    void deleteNotifica_Ritorna204() throws Exception {
        mockMvc.perform(delete("/api/sistema/notifiche/1"))
                .andExpect(status().isNoContent());

        verify(notificaService).eliminaNotifica(1);
    }

    @Test
    void getAllLogs_RitornaLista200() throws Exception {
        LogSincronizzazione log = new LogSincronizzazione();
        LogSincronizzazioneDTO dto = new LogSincronizzazioneDTO();
        dto.setIdLog(100);

        when(logService.findAll()).thenReturn(List.of(log));
        when(logService.convertToDTO(log)).thenReturn(dto);

        mockMvc.perform(get("/api/sistema/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLog").value(100));
    }

    // System Observability: verifica l'esposizione filtrata degli Audit Log (livello ERROR), garantendo agli amministratori il monitoraggio proattivo delle anomalie infrastrutturali
    @Test
    void getErrorLogs_RitornaLista200() throws Exception {
        LogSincronizzazione log = new LogSincronizzazione();
        LogSincronizzazioneDTO dto = new LogSincronizzazioneDTO();
        dto.setIdLog(101);

        when(logService.trovaErrori()).thenReturn(List.of(log));
        when(logService.convertToDTO(log)).thenReturn(dto);

        mockMvc.perform(get("/api/sistema/logs/errori"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLog").value(101));
    }

    @Test
    void createLog_Valido_Ritorna201() throws Exception {
        LogSincronizzazione request = new LogSincronizzazione();
        request.setDescrizioneEvento("Test Evento");
        request.setEsitoPositivo(true);
        request.setNoteTecniche("Dettagli");

        LogSincronizzazione logSalvato = new LogSincronizzazione();
        LogSincronizzazioneDTO dto = new LogSincronizzazioneDTO();
        dto.setIdLog(102);

        when(logService.registraEvento("Test Evento", true, "Dettagli")).thenReturn(logSalvato);
        when(logService.convertToDTO(logSalvato)).thenReturn(dto);

        mockMvc.perform(post("/api/sistema/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idLog").value(102));
    }

    @Test
    void pulisciLogVecchi_DefaultGiorni_Ritorna204() throws Exception {
        mockMvc.perform(delete("/api/sistema/logs/pulizia"))
                .andExpect(status().isNoContent());

        verify(logService).pulisciLogVecchi(any(LocalDateTime.class));
    }

    // Data Lifecycle Management (Housekeeping): collauda l'endpoint di pulizia programmata, assicurando che il parametro di retention (giorni) piloti correttamente la cancellazione fisica dei log
    @Test
    void pulisciLogVecchi_ParametroGiorni_Ritorna204() throws Exception {
        mockMvc.perform(delete("/api/sistema/logs/pulizia")
                        .param("giorniVecchiaia", "15"))
                .andExpect(status().isNoContent());

        verify(logService).pulisciLogVecchi(any(LocalDateTime.class));
    }
}
