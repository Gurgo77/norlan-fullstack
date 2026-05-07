package it.norlan.clientportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.norlan.clientportal.dto.AssegnazioneDPIDTO;
import it.norlan.clientportal.dto.DipendenteDTO;
import it.norlan.clientportal.model.AssegnazioneDPI;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.service.AssegnazioneDPIService;
import it.norlan.clientportal.service.DipendenteService;
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
class LavoratoreControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DipendenteService dipendenteService;

    @Mock
    private AssegnazioneDPIService dpiService;

    @InjectMocks
    private LavoratoreController lavoratoreController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(lavoratoreController).build();
    }

    @Test
    void getAllDipendenti_RitornaLista200() throws Exception {
        Dipendente dipendente = new Dipendente();
        DipendenteDTO dto = new DipendenteDTO();
        dto.setIdUtente(1);

        when(dipendenteService.findAll()).thenReturn(List.of(dipendente));
        when(dipendenteService.convertToDTO(dipendente)).thenReturn(dto);

        mockMvc.perform(get("/api/lavoratori"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUtente").value(1));
    }

    @Test
    void getDipendenteById_Trovato_Ritorna200() throws Exception {
        Dipendente dipendente = new Dipendente();
        DipendenteDTO dto = new DipendenteDTO();
        dto.setIdUtente(2);

        when(dipendenteService.findById(2)).thenReturn(Optional.of(dipendente));
        when(dipendenteService.convertToDTO(dipendente)).thenReturn(dto);

        mockMvc.perform(get("/api/lavoratori/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUtente").value(2));
    }

    @Test
    void getDipendenteById_NonTrovato_Ritorna404() throws Exception {
        when(dipendenteService.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/lavoratori/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDipendentiByAzienda_RitornaLista200() throws Exception {
        Dipendente dipendente = new Dipendente();
        DipendenteDTO dto = new DipendenteDTO();
        dto.setIdUtente(3);

        when(dipendenteService.findByAzienda(10)).thenReturn(List.of(dipendente));
        when(dipendenteService.convertToDTO(dipendente)).thenReturn(dto);

        mockMvc.perform(get("/api/lavoratori/azienda/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUtente").value(3));
    }

    @Test
    void createDipendente_DatiValidi_Ritorna201() throws Exception {
        Dipendente request = new Dipendente();
        request.setNome("Mario");
        request.setCognome("Rossi");

        Dipendente salvato = new Dipendente();
        DipendenteDTO dto = new DipendenteDTO();
        dto.setIdUtente(4);
        dto.setNome("Mario");

        when(dipendenteService.salvaDipendente(any(Dipendente.class), eq(10))).thenReturn(salvato);
        when(dipendenteService.convertToDTO(salvato)).thenReturn(dto);

        mockMvc.perform(post("/api/lavoratori/azienda/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUtente").value(4))
                .andExpect(jsonPath("$.nome").value("Mario"));
    }

    @Test
    void createDipendente_ErroreDatabase_Ritorna400() throws Exception {
        Dipendente request = new Dipendente();

        when(dipendenteService.salvaDipendente(any(Dipendente.class), eq(10)))
                .thenThrow(new RuntimeException("Azienda non trovata"));

        mockMvc.perform(post("/api/lavoratori/azienda/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDipendente_Trovato_Ritorna200() throws Exception {
        Dipendente dipendenteEsistente = new Dipendente();
        Azienda azienda = new Azienda();
        azienda.setIdUtente(10);
        dipendenteEsistente.setAzienda(azienda);

        Dipendente dipendenteDati = new Dipendente();
        dipendenteDati.setNome("Luigi");
        dipendenteDati.setEmail("luigi@test.it");

        Dipendente dipendenteAggiornato = new Dipendente();
        DipendenteDTO dto = new DipendenteDTO();
        dto.setNome("Luigi");

        when(dipendenteService.findById(5)).thenReturn(Optional.of(dipendenteEsistente));
        when(dipendenteService.salvaDipendente(any(Dipendente.class), eq(10))).thenReturn(dipendenteAggiornato);
        when(dipendenteService.convertToDTO(dipendenteAggiornato)).thenReturn(dto);

        mockMvc.perform(put("/api/lavoratori/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dipendenteDati)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Luigi"));
    }

    @Test
    void updateDipendente_NonTrovato_Ritorna404() throws Exception {
        when(dipendenteService.findById(5)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/lavoratori/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Dipendente())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDipendente_Trovato_Ritorna204() throws Exception {
        when(dipendenteService.findById(5)).thenReturn(Optional.of(new Dipendente()));

        mockMvc.perform(delete("/api/lavoratori/5"))
                .andExpect(status().isNoContent());

        verify(dipendenteService).eliminaDipendente(5);
    }

    @Test
    void deleteDipendente_NonTrovato_Ritorna404() throws Exception {
        when(dipendenteService.findById(5)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/lavoratori/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDpiByDipendente_RitornaLista200() throws Exception {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        AssegnazioneDPIDTO dto = new AssegnazioneDPIDTO();
        dto.setIdAssegnazione(100);

        when(dpiService.trovaPerDipendente(5)).thenReturn(List.of(dpi));
        when(dpiService.convertToDTO(dpi)).thenReturn(dto);

        mockMvc.perform(get("/api/lavoratori/5/dpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAssegnazione").value(100));
    }

    @Test
    void assignDpiToDipendente_DipendenteTrovato_Ritorna201() throws Exception {
        Dipendente dipendente = new Dipendente();
        AssegnazioneDPI assegnazione = new AssegnazioneDPI();
        AssegnazioneDPI assegnazioneSalvata = new AssegnazioneDPI();
        AssegnazioneDPIDTO dto = new AssegnazioneDPIDTO();
        dto.setIdAssegnazione(101);

        when(dipendenteService.findById(5)).thenReturn(Optional.of(dipendente));
        when(dpiService.salvaAssegnazione(any(AssegnazioneDPI.class))).thenReturn(assegnazioneSalvata);
        when(dpiService.convertToDTO(assegnazioneSalvata)).thenReturn(dto);

        mockMvc.perform(post("/api/lavoratori/5/dpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assegnazione)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAssegnazione").value(101));
    }

    @Test
    void assignDpiToDipendente_DipendenteNonTrovato_Ritorna404() throws Exception {
        when(dipendenteService.findById(5)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/lavoratori/5/dpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssegnazioneDPI())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDpiInScadenza_RitornaLista200() throws Exception {
        AssegnazioneDPI dpi = new AssegnazioneDPI();
        AssegnazioneDPIDTO dto = new AssegnazioneDPIDTO();
        dto.setIdAssegnazione(102);

        when(dpiService.trovaInScadenzaRevisione(30)).thenReturn(List.of(dpi));
        when(dpiService.convertToDTO(dpi)).thenReturn(dto);

        mockMvc.perform(get("/api/lavoratori/dpi/in-scadenza")
                        .param("giorni", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAssegnazione").value(102));
    }

    @Test
    void deleteDpiAssignment_Trovato_Ritorna204() throws Exception {
        when(dpiService.findById(100)).thenReturn(Optional.of(new AssegnazioneDPI()));

        mockMvc.perform(delete("/api/lavoratori/dpi/100"))
                .andExpect(status().isNoContent());

        verify(dpiService).eliminaAssegnazione(100);
    }

    @Test
    void deleteDpiAssignment_NonTrovato_Ritorna404() throws Exception {
        when(dpiService.findById(100)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/lavoratori/dpi/100"))
                .andExpect(status().isNotFound());
    }
}
