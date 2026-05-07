package it.norlan.clientportal.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.norlan.clientportal.dto.AuthRequestDTO;
import it.norlan.clientportal.dto.CambioPasswordDTO;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.security.JwtUtil;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import it.norlan.clientportal.service.UtenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LogSincronizzazioneService logService;

    @Mock
    private UtenteService utenteService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_CredenzialiCorrette_RitornaTokenEStatus200() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("admin@norlan.it");
        request.setPassword("password123");

        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        Admin utenteMock = new Admin();
        utenteMock.setIdUtente(1);
        utenteMock.setEmail("admin@norlan.it");
        utenteMock.setRuolo(Utente.Ruolo.ADMIN);
        utenteMock.setRichiedeCambioPassword(false);

        when(utenteService.findByEmail("admin@norlan.it")).thenReturn(Optional.of(utenteMock));
        when(jwtUtil.generateToken(utenteMock)).thenReturn("jwt.token.mock");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.mock"))
                .andExpect(jsonPath("$.idUtente").value(1))
                .andExpect(jsonPath("$.email").value("admin@norlan.it"))
                .andExpect(jsonPath("$.ruolo").value("ADMIN"))
                .andExpect(jsonPath("$.richiedeCambioPassword").value(false));

        verify(logService).registraEvento(eq("Accesso al portale"), eq(true), anyString());
    }

    @Test
    void login_CredenzialiErrate_Ritorna401() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("admin@norlan.it");
        request.setPassword("sbagliata");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Credenziali non valide"));

        verify(logService).registraEvento(eq("Tentativo di accesso fallito"), eq(false), anyString());
    }

    @Test
    void login_UtenteAutenticatoMaNonNelDB_Ritorna401() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("nodb@norlan.it");
        request.setPassword("password123");

        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        when(utenteService.findByEmail("nodb@norlan.it")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Utente non trovato"));
    }

    @Test
    void cambiaPassword_Successo_Ritorna200() throws Exception {
        CambioPasswordDTO request = new CambioPasswordDTO();
        request.setVecchiaPassword("oldPass");
        request.setNuovaPassword("newPass");

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken principal =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user@norlan.it",
                        null,
                        java.util.Collections.emptyList()
                );

        mockMvc.perform(put("/api/auth/cambia-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string("Password aggiornata con successo."));

        verify(utenteService).cambiaPassword("user@norlan.it", "oldPass", "newPass");
        verify(logService).registraEvento(eq("Aggiornamento credenziali di sicurezza"), eq(true), anyString());
    }

    @Test
    void cambiaPassword_VecchiaPasswordErrata_Ritorna401() throws Exception {
        CambioPasswordDTO request = new CambioPasswordDTO();
        request.setVecchiaPassword("errata");
        request.setNuovaPassword("newPass");

        doThrow(new BadCredentialsException("La vecchia password non   corretta"))
                .when(utenteService).cambiaPassword("user@norlan.it", "errata", "newPass");

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken principal =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user@norlan.it",
                        null,
                        java.util.Collections.emptyList()
                );

        mockMvc.perform(put("/api/auth/cambia-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(principal))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("La vecchia password non   corretta"));
    }

    @Test
    void cambiaPassword_ErroreGenerico_Ritorna500() throws Exception {
        doThrow(new RuntimeException("Errore imprevisto"))
                .when(utenteService).cambiaPassword(any(), any(), any());

        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "utente@test.it",
                        null,
                        java.util.Collections.emptyList()
                );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/cambia-password")
                        .principal(auth)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{ \"vecchiaPassword\": \"old\", \"nuovaPassword\": \"new\" }"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    void logout_Ritorna200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logout effettuato con successo. Il client deve eliminare il token."));
    }
}
