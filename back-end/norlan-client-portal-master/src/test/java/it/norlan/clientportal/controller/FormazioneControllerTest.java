package it.norlan.clientportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.norlan.clientportal.dto.CorsoFormazioneDTO;
import it.norlan.clientportal.dto.IscrizioneCorsoDTO;
import it.norlan.clientportal.dto.MaterialeDidatticoDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.model.MaterialeDidattico;
import it.norlan.clientportal.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FormazioneControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CorsoFormazioneService corsoService;

    @Mock
    private IscrizioneCorsoService iscrizioneService;

    @Mock
    private MaterialeDidatticoService materialeService;

    @Mock
    private UtenteService utenteService;

    @Mock
    private DocumentoService documentoService;

    @InjectMocks
    private FormazioneController formazioneController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(formazioneController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllCorsi_RitornaLista200() throws Exception {
        CorsoFormazione corso = new CorsoFormazione();
        CorsoFormazioneDTO dto = new CorsoFormazioneDTO();
        dto.setIdCorso(1);

        when(corsoService.findAll()).thenReturn(List.of(corso));
        when(corsoService.convertToDTO(corso)).thenReturn(dto);

        mockMvc.perform(get("/api/formazione/corsi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCorso").value(1));
    }

    @Test
    void getCorsoById_Trovato_Ritorna200() throws Exception {
        CorsoFormazione corso = new CorsoFormazione();
        CorsoFormazioneDTO dto = new CorsoFormazioneDTO();
        dto.setIdCorso(2);

        when(corsoService.findById(2)).thenReturn(Optional.of(corso));
        when(corsoService.convertToDTO(corso)).thenReturn(dto);

        mockMvc.perform(get("/api/formazione/corsi/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCorso").value(2));
    }

    @Test
    void getCorsoById_NonTrovato_Ritorna404() throws Exception {
        when(corsoService.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/formazione/corsi/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCorso_Valido_Ritorna201() throws Exception {
        CorsoFormazione corso = new CorsoFormazione();
        CorsoFormazione corsoSalvato = new CorsoFormazione();
        CorsoFormazioneDTO dto = new CorsoFormazioneDTO();
        dto.setIdCorso(3);

        when(corsoService.salvaCorso(any(CorsoFormazione.class))).thenReturn(corsoSalvato);
        when(corsoService.convertToDTO(corsoSalvato)).thenReturn(dto);

        mockMvc.perform(post("/api/formazione/corsi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corso)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCorso").value(3));
    }

    @Test
    void createCorso_DatiNonValidi_Ritorna400() throws Exception {
        when(corsoService.salvaCorso(any(CorsoFormazione.class))).thenThrow(new IllegalArgumentException("Data passata"));

        mockMvc.perform(post("/api/formazione/corsi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorsoFormazione())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatoCorso_Trovato_Ritorna200() throws Exception {
        when(corsoService.findById(1)).thenReturn(Optional.of(new CorsoFormazione()));

        mockMvc.perform(patch("/api/formazione/corsi/1/stato")
                        .param("nuovoStato", "IN_SVOLGIMENTO"))
                .andExpect(status().isOk());

        verify(corsoService).aggiornaStato(1, CorsoFormazione.StatoCorso.IN_SVOLGIMENTO);
    }

    @Test
    void deleteCorso_Trovato_Ritorna204() throws Exception {
        when(corsoService.findById(1)).thenReturn(Optional.of(new CorsoFormazione()));

        mockMvc.perform(delete("/api/formazione/corsi/1"))
                .andExpect(status().isNoContent());

        verify(corsoService).eliminaCorso(1);
    }

    @Test
    void iscriviUtente_Valido_Ritorna201() throws Exception {
        IscrizioneCorso iscrizione = new IscrizioneCorso();
        IscrizioneCorsoDTO dto = new IscrizioneCorsoDTO();
        dto.setIdCorso(1);
        dto.setIdUtente(10);

        when(iscrizioneService.iscriviUtente(10, 1)).thenReturn(iscrizione);
        when(iscrizioneService.convertToDTO(iscrizione)).thenReturn(dto);

        mockMvc.perform(post("/api/formazione/corsi/1/iscrizioni/10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCorso").value(1))
                .andExpect(jsonPath("$.idUtente").value(10));
    }

    @Test
    void iscriviUtente_GiaIscritto_Ritorna409() throws Exception {
        when(iscrizioneService.iscriviUtente(10, 1)).thenThrow(new IllegalStateException("Utente gia iscritto"));

        mockMvc.perform(post("/api/formazione/corsi/1/iscrizioni/10"))
                .andExpect(status().isConflict());
    }

    @Test
    void validaPresenzeAdmin_Ritorna200() throws Exception {
        List<Integer> presenti = List.of(1, 2, 3);

        mockMvc.perform(post("/api/formazione/corsi/1/valida-presenze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presenti)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Presenze validate. Registro in attesa di firma docente."));

        verify(corsoService).validaPresenzeAdmin(1, presenti);
    }

    @Test
    void controfirmaRegistro_Valido_Ritorna200() throws Exception {
        Docente docente = new Docente();
        docente.setIdUtente(5);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("docente@test.it");
        when(utenteService.findByEmail("docente@test.it")).thenReturn(Optional.of(docente));

        mockMvc.perform(post("/api/formazione/corsi/1/firma-docente")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro controfirmato con successo."));

        verify(corsoService).controfirmaDocente(1, 5);
    }

    @Test
    void distribuisciAttestati_LunghezzaMismatch_Ritorna400() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.pdf", MediaType.APPLICATION_PDF_VALUE, "dati".getBytes());

        mockMvc.perform(multipart("/api/formazione/corsi/1/distribuisci-attestati")
                        .file(file1)
                        .param("idAziende", "10", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mismatch tra file caricati e aziende."));
    }

    @Test
    void distribuisciAttestati_Validi_Ritorna200() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.pdf", MediaType.APPLICATION_PDF_VALUE, "dati".getBytes());

        when(fileStorageService.storeFile(any(), anyString())).thenReturn("path/to/file.pdf");

        mockMvc.perform(multipart("/api/formazione/corsi/1/distribuisci-attestati")
                        .file(file1)
                        .param("idAziende", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Attestati distribuiti con successo."));

        verify(documentoService).distribuisciAttestatiMassivi(eq(1), any());
    }

    @Test
    void downloadCertificato_Esistente_RitornaFilePdf() throws Exception {
        Resource mockResource = new ByteArrayResource(new byte[]{1, 2}) {
            @Override
            public String getFilename() { return "cert.pdf"; }
            @Override
            public boolean exists() { return true; }
            @Override
            public boolean isReadable() { return true; }
        };

        when(iscrizioneService.getPathAttestato(10, 1)).thenReturn("path/file.pdf");
        when(fileStorageService.loadFileAsResource("path/file.pdf")).thenReturn(mockResource);

        mockMvc.perform(get("/api/formazione/corsi/1/iscrizioni/10/certificato/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Attestato_10.pdf\""));
    }

    @Test
    void uploadMateriale_CorsoTrovato_Ritorna201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slide.pdf", MediaType.APPLICATION_PDF_VALUE, "dati".getBytes());
        CorsoFormazione corso = new CorsoFormazione();
        MaterialeDidattico materiale = new MaterialeDidattico();
        MaterialeDidatticoDTO dto = new MaterialeDidatticoDTO();
        dto.setIdMateriale(50);

        when(corsoService.findById(1)).thenReturn(Optional.of(corso));
        when(fileStorageService.storeFile(any(), anyString())).thenReturn("path/to/slide.pdf");
        when(materialeService.salvaMateriale(any())).thenReturn(materiale);
        when(materialeService.convertToDTO(materiale)).thenReturn(dto);

        mockMvc.perform(multipart("/api/formazione/corsi/1/materiali")
                        .file(file)
                        .param("titoloDocumento", "Slide Capitolo 1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMateriale").value(50));
    }

    @Test
    void deleteMateriale_Trovato_Ritorna204() throws Exception {
        when(materialeService.findById(1)).thenReturn(Optional.of(new MaterialeDidattico()));

        mockMvc.perform(delete("/api/formazione/materiali/1"))
                .andExpect(status().isNoContent());

        verify(materialeService).eliminaMateriale(1);
    }
}
