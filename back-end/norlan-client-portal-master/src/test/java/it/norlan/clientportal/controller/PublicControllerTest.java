package it.norlan.clientportal.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.norlan.clientportal.dto.ContattoWebDTO;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.service.AdminService;
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
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdminService adminService;

    @Mock
    private NotificaService notificaService;

    @InjectMocks
    private PublicController publicController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void riceviContatto_Valido_Ritorna200() throws Exception {
        ContattoWebDTO dto = new ContattoWebDTO();
        dto.setNome("Mario");
        dto.setCognome("Rossi");
        dto.setEmail("mario.rossi@test.it");
        dto.setMessaggio("Richiesta di informazioni sui corsi.");
        dto.setPrivacyAccepted(true);

        Admin adminMock = new Admin();
        adminMock.setIdUtente(1);
        adminMock.setEmail("admin@norlan.it");

        when(adminService.getUnicoAdmin()).thenReturn(Optional.of(adminMock));

        mockMvc.perform(post("/api/public/contatto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Messaggio inviato con successo!"));

        String expectedCorpoMail = "Nuovo messaggio dal form web di NorLan:\n\n"
                + "Nome: Mario Rossi\n"
                + "Email di contatto: mario.rossi@test.it\n\n"
                + "Messaggio:\nRichiesta di informazioni sui corsi.";

        verify(notificaService).inviaNotifica(
                eq(adminMock),
                eq(expectedCorpoMail),
                eq(Notifica.Priorita.ALTA),
                eq(Notifica.CanaleNotifica.EMAIL)
        );
    }

    @Test
    void riceviContatto_PrivacyNonAccettata_Ritorna400() throws Exception {
        ContattoWebDTO dto = new ContattoWebDTO();
        dto.setNome("Luigi");
        dto.setPrivacyAccepted(false);

        mockMvc.perform(post("/api/public/contatto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Devi accettare la privacy."));

        verifyNoInteractions(adminService);
        verifyNoInteractions(notificaService);
    }

    @Test
    void riceviContatto_AdminNonTrovato_LanciaEccezione() {
        ContattoWebDTO dto = new ContattoWebDTO();
        dto.setNome("Anna");
        dto.setPrivacyAccepted(true);

        when(adminService.getUnicoAdmin()).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/public/contatto")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)));
        });

        assertTrue(exception.getCause().getMessage().contains("Sistema non configurato"),
                "La root cause deve corrispondere all'eccezione della logica di business");

        verifyNoInteractions(notificaService);
    }
}
