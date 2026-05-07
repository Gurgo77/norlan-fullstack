package it.norlan.clientportal.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.norlan.clientportal.dto.*;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.service.*;
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
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnagraficaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RegistrazioneService registrazioneService;

    @Mock
    private AziendaService aziendaService;

    @Mock
    private DocenteService docenteService;

    @Mock
    private AdminService adminService;

    @Mock
    private DipendenteService dipendenteService;

    @InjectMocks
    private AnagraficaController anagraficaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(anagraficaController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registraNuovoUtente_Ritorna201() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("test@test.it");
        request.setPassword("password");
        request.setRuolo(Utente.Ruolo.AZIENDA);

        Azienda utenteSalvato = new Azienda();
        utenteSalvato.setRuolo(Utente.Ruolo.AZIENDA);

        when(registrazioneService.registraNuovoUtente(any(AuthRequestDTO.class))).thenReturn(utenteSalvato);

        mockMvc.perform(post("/api/anagrafica/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registrazione completata con successo. Ruolo assegnato: AZIENDA"));
    }

    @Test
    void registraNuovoUtente_Eccezione_Ritorna400() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();

        when(registrazioneService.registraNuovoUtente(any())).thenThrow(new RuntimeException("Errore custom"));

        mockMvc.perform(post("/api/anagrafica/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Errore durante la registrazione: Errore custom"));
    }

    @Test
    void getAllAziende_RitornaLista200() throws Exception {
        Azienda azienda = new Azienda();
        azienda.setIdUtente(1);
        AziendaDTO dto = new AziendaDTO();
        dto.setIdUtente(1);

        when(aziendaService.findAll()).thenReturn(List.of(azienda));
        when(aziendaService.convertToDTO(azienda)).thenReturn(dto);

        mockMvc.perform(get("/api/anagrafica/aziende"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUtente").value(1));
    }

    @Test
    void getAziendaById_Trovata_Ritorna200() throws Exception {
        Azienda azienda = new Azienda();
        AziendaDTO dto = new AziendaDTO();
        dto.setIdUtente(1);

        when(aziendaService.findById(1)).thenReturn(Optional.of(azienda));
        when(aziendaService.convertToDTO(azienda)).thenReturn(dto);

        mockMvc.perform(get("/api/anagrafica/aziende/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUtente").value(1));
    }

    @Test
    void getAziendaById_NonTrovata_Ritorna404() throws Exception {
        when(aziendaService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/anagrafica/aziende/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAzienda_Trovata_Ritorna200() throws Exception {
        Azienda aziendaEsistente = new Azienda();
        AziendaDTO requestDto = new AziendaDTO();
        requestDto.setRagioneSociale("Nuova Ragione");
        requestDto.setEmail("nuova@email.it");

        Azienda aziendaAggiornata = new Azienda();
        AziendaDTO responseDto = new AziendaDTO();
        responseDto.setRagioneSociale("Nuova Ragione");

        when(aziendaService.findById(1)).thenReturn(Optional.of(aziendaEsistente));
        when(aziendaService.salvaAzienda(any(Azienda.class))).thenReturn(aziendaAggiornata);
        when(aziendaService.convertToDTO(aziendaAggiornata)).thenReturn(responseDto);

        mockMvc.perform(put("/api/anagrafica/aziende/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ragioneSociale").value("Nuova Ragione"));
    }

    @Test
    void updateAzienda_NonTrovata_Ritorna404() throws Exception {
        when(aziendaService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/anagrafica/aziende/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AziendaDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAzienda_Trovata_Ritorna204() throws Exception {
        when(aziendaService.findById(1)).thenReturn(Optional.of(new Azienda()));

        mockMvc.perform(delete("/api/anagrafica/aziende/1"))
                .andExpect(status().isNoContent());

        verify(aziendaService).eliminaAzienda(1);
    }

    @Test
    void deleteAzienda_NonTrovata_Ritorna404() throws Exception {
        when(aziendaService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/anagrafica/aziende/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllDocenti_RitornaLista200() throws Exception {
        Docente docente = new Docente();
        DocenteDTO dto = new DocenteDTO();
        dto.setIdUtente(2);

        when(docenteService.findAll()).thenReturn(List.of(docente));
        when(docenteService.convertToDTO(docente)).thenReturn(dto);

        mockMvc.perform(get("/api/anagrafica/docenti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUtente").value(2));
    }

    @Test
    void getDocenteById_Trovato_Ritorna200() throws Exception {
        Docente docente = new Docente();
        DocenteDTO dto = new DocenteDTO();
        dto.setIdUtente(2);

        when(docenteService.findById(2)).thenReturn(Optional.of(docente));
        when(docenteService.convertToDTO(docente)).thenReturn(dto);

        mockMvc.perform(get("/api/anagrafica/docenti/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUtente").value(2));
    }

    @Test
    void getDocenteById_NonTrovato_Ritorna404() throws Exception {
        when(docenteService.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/anagrafica/docenti/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocente_Trovato_Ritorna200() throws Exception {
        Docente docenteEsistente = new Docente();
        Docente request = new Docente();
        request.setSpecializzazioneTecnica("Sicurezza");

        Docente docenteAggiornato = new Docente();
        DocenteDTO responseDto = new DocenteDTO();
        responseDto.setSpecializzazioneTecnica("Sicurezza");

        when(docenteService.findById(2)).thenReturn(Optional.of(docenteEsistente));
        when(docenteService.salvaDocente(any(Docente.class))).thenReturn(docenteAggiornato);
        when(docenteService.convertToDTO(docenteAggiornato)).thenReturn(responseDto);

        mockMvc.perform(put("/api/anagrafica/docenti/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specializzazioneTecnica").value("Sicurezza"));
    }

    @Test
    void updateDocente_NonTrovato_Ritorna404() throws Exception {
        when(docenteService.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/anagrafica/docenti/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Docente())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocente_Trovato_Ritorna204() throws Exception {
        when(docenteService.findById(2)).thenReturn(Optional.of(new Docente()));

        mockMvc.perform(delete("/api/anagrafica/docenti/2"))
                .andExpect(status().isNoContent());

        verify(docenteService).eliminaDocente(2);
    }

    @Test
    void deleteDocente_NonTrovato_Ritorna404() throws Exception {
        when(docenteService.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/anagrafica/docenti/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAdmin_Trovato_Ritorna200() throws Exception {
        Admin admin = new Admin();
        AdminDTO dto = new AdminDTO();
        dto.setIdUtente(3);

        when(adminService.getUnicoAdmin()).thenReturn(Optional.of(admin));
        when(adminService.convertToDTO(admin)).thenReturn(dto);

        mockMvc.perform(get("/api/anagrafica/admin/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUtente").value(3));
    }

    @Test
    void getAdmin_NonTrovato_Ritorna404() throws Exception {
        when(adminService.getUnicoAdmin()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/anagrafica/admin/3"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAdmin_Trovato_Ritorna200() throws Exception {
        AdminDTO requestDto = new AdminDTO();
        requestDto.setEmail("admin@test.it");

        Admin adminAggiornato = new Admin();
        AdminDTO responseDto = new AdminDTO();
        responseDto.setEmail("admin@test.it");

        when(adminService.aggiornaAdmin(eq(3), any(AdminDTO.class))).thenReturn(adminAggiornato);
        when(adminService.convertToDTO(adminAggiornato)).thenReturn(responseDto);

        mockMvc.perform(put("/api/anagrafica/admin/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.it"));
    }

    @Test
    void updateAdmin_Eccezione_Ritorna404() throws Exception {
        when(adminService.aggiornaAdmin(eq(3), any())).thenThrow(new RuntimeException("Admin non trovato"));

        mockMvc.perform(put("/api/anagrafica/admin/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllDipendenti_RitornaLista200() throws Exception {
        Dipendente dipendente = new Dipendente();
        DipendenteDTO dto = new DipendenteDTO();
        dto.setIdUtente(4);

        when(dipendenteService.findAll()).thenReturn(List.of(dipendente));
        when(dipendenteService.convertToDTO(dipendente)).thenReturn(dto);

        mockMvc.perform(get("/api/anagrafica/dipendenti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUtente").value(4));
    }
}
