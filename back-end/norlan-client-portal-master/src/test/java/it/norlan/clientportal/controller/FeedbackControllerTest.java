package it.norlan.clientportal.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.norlan.clientportal.dto.FeedbackDTO;
import it.norlan.clientportal.dto.FeedbackStatsDTO;
import it.norlan.clientportal.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackController feedbackController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feedbackController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void inviaFeedback_Valido_Ritorna200() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setIdUtente(1);
        request.setIdCorso(10);
        request.setRatingDocenza(5);
        request.setRatingContenuti(4);
        request.setCommento("Ottimo corso");

        mockMvc.perform(post("/api/feedback/invia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Feedback archiviato con successo e metriche aggiornate."));
    }

    @Test
    void inviaFeedback_StatoIllegale_Ritorna400() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setIdUtente(1);
        request.setIdCorso(10);
        request.setRatingDocenza(5);
        request.setRatingContenuti(5);

        doThrow(new IllegalStateException("Violazione: Impossibile inviare feedback per un corso non ancora terminato."))
                .when(feedbackService).registraFeedback(any(FeedbackDTO.class));

        mockMvc.perform(post("/api/feedback/invia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Violazione: Impossibile inviare feedback per un corso non ancora terminato."));
    }

    @Test
    void inviaFeedback_ErroreGenerico_Ritorna500() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setIdUtente(1);
        request.setIdCorso(10);
        request.setRatingDocenza(5);
        request.setRatingContenuti(5);

        doThrow(new RuntimeException("Database down"))
                .when(feedbackService).registraFeedback(any(FeedbackDTO.class));

        mockMvc.perform(post("/api/feedback/invia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Errore di sistema durante il salvataggio del feedback."));
    }

    @Test
    void getStatisticheCorso_Trovate_Ritorna200() throws Exception {
        FeedbackStatsDTO mockStats = new FeedbackStatsDTO();
        mockStats.setIdCorso(10);
        mockStats.setMediaDocenza(4.5);
        mockStats.setMediaContenuti(4.0);
        mockStats.setTotaleFeedback(2L);
        mockStats.setCommenti(List.of("Bello", "Utile"));

        when(feedbackService.getStatisticheCorso(10)).thenReturn(mockStats);

        mockMvc.perform(get("/api/feedback/corso/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCorso").value(10))
                .andExpect(jsonPath("$.mediaDocenza").value(4.5))
                .andExpect(jsonPath("$.mediaContenuti").value(4.0))
                .andExpect(jsonPath("$.totaleFeedback").value(2));
    }

    @Test
    void getStatisticheCorso_ErroreGenerico_Ritorna500() throws Exception {
        when(feedbackService.getStatisticheCorso(10)).thenThrow(new RuntimeException("Errore DB"));

        mockMvc.perform(get("/api/feedback/corso/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Errore durante il recupero delle statistiche del feedback."));
    }
}
