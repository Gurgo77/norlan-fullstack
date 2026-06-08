package it.norlan.clientportal.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.norlan.clientportal.dto.DocumentoDTO;
import it.norlan.clientportal.dto.RichiestaRinnovoDocumentoDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import it.norlan.clientportal.service.AziendaService;
import it.norlan.clientportal.service.DocumentoService;
import it.norlan.clientportal.service.FileStorageService;
import it.norlan.clientportal.service.RichiestaRinnovoDocumentoService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite di collaudo (Unit Test) per il layer REST di gestione documentale.
 * Verifica le operazioni di I/O (Upload/Download di MultipartFile) tramite simulazione in memoria
 * e collauda l'esposizione web della Macchina a Stati Finiti (FSM) del dominio documentale.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private RichiestaRinnovoDocumentoService rinnovoService;

    @Mock
    private AziendaService aziendaService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DocumentoController documentoController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(documentoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllDocumenti_RitornaLista200() throws Exception {
        Documento doc = new Documento();
        DocumentoDTO dto = new DocumentoDTO();
        dto.setIdDocumento(1);

        when(documentoService.findAll()).thenReturn(List.of(doc));
        when(documentoService.convertToDTO(doc)).thenReturn(dto);

        mockMvc.perform(get("/api/documenti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idDocumento").value(1));
    }

    @Test
    void getDocumentoById_Trovato_Ritorna200() throws Exception {
        Documento doc = new Documento();
        DocumentoDTO dto = new DocumentoDTO();
        dto.setIdDocumento(2);

        when(documentoService.findById(2)).thenReturn(Optional.of(doc));
        when(documentoService.convertToDTO(doc)).thenReturn(dto);

        mockMvc.perform(get("/api/documenti/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idDocumento").value(2));
    }

    @Test
    void getDocumentoById_NonTrovato_Ritorna404() throws Exception {
        when(documentoService.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documenti/2"))
                .andExpect(status().isNotFound());
    }

    // Collauda lo streaming binario (Download): simula un file system virtuale (ByteArrayResource) per verificare l'impostazione corretta degli header HTTP (es. Content-Disposition)
    @Test
    void downloadDocumento_Trovato_RitornaFilePdf() throws Exception {
        Documento doc = new Documento();
        doc.setFilePath("test/path.pdf");

        Resource mockResource = new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "test.pdf";
            }
        };

        when(documentoService.findById(1)).thenReturn(Optional.of(doc));
        when(fileStorageService.loadFileAsResource("test/path.pdf")).thenReturn(mockResource);

        mockMvc.perform(get("/api/documenti/1/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.pdf\""));
    }

    // Verifica l'ingestione dei dati (Upload): sfrutta MockMultipartFile per simulare un payload multipart/form-data, isolando il test dal file system fisico
    @Test
    void uploadDocumento_DatiValidi_Ritorna201() throws Exception {
        Azienda azienda = new Azienda();
        azienda.setIdUtente(1);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "dati".getBytes());

        Documento documentoSalvato = new Documento();
        DocumentoDTO dto = new DocumentoDTO();
        dto.setIdDocumento(10);

        when(aziendaService.findById(1)).thenReturn(Optional.of(azienda));
        when(fileStorageService.storeFile(any(), anyString())).thenReturn("aziende/azienda_1/test.pdf");
        when(documentoService.salvaDocumento(any(Documento.class))).thenReturn(documentoSalvato);
        when(documentoService.convertToDTO(documentoSalvato)).thenReturn(dto);

        mockMvc.perform(multipart("/api/documenti/azienda/1/upload")
                        .file(file)
                        .param("modulo", "SICUREZZA")
                        .param("tipologia", "DVR")
                        .param("dataScadenza", "2026-12-31"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDocumento").value(10));
    }

    @Test
    void uploadDocumento_AziendaNonTrovata_Ritorna400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "dati".getBytes());

        when(aziendaService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/api/documenti/azienda/99/upload")
                        .file(file)
                        .param("modulo", "SICUREZZA")
                        .param("tipologia", "DVR")
                        .param("dataScadenza", "2026-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Azienda non trovata con ID: 99"));
    }

    @Test
    void richiediFirma_Effettuata_Ritorna200() throws Exception {
        mockMvc.perform(patch("/api/documenti/1/richiedi-firma"))
                .andExpect(status().isOk());
        verify(documentoService).richiediFirmaDocumento(1);
    }

    @Test
    void approva_Effettuata_Ritorna200() throws Exception {
        mockMvc.perform(patch("/api/documenti/1/approva"))
                .andExpect(status().isOk());
        // CORREZIONE: aggiunto 'null' come secondo parametro atteso
        verify(documentoService).approvaDocumento(1, null);
    }

    @Test
    void archivia_Effettuata_Ritorna200() throws Exception {
        mockMvc.perform(patch("/api/documenti/1/archivia"))
                .andExpect(status().isOk());
        verify(documentoService).archiviaDocumento(1);
    }

    // Integrazione FSM ed Exception Handling: assicura che il rigetto di una transizione di stato a livello di dominio venga intercettato e tradotto in un HTTP 400 (Bad Request)
    @Test
    void archivia_StatoIllegale_Ritorna400() throws Exception {
        doThrow(new IllegalStateException("Transizione non permessa")).when(documentoService).archiviaDocumento(1);

        mockMvc.perform(patch("/api/documenti/1/archivia"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Transizione non permessa"));
    }

    @Test
    void deleteDocumento_Trovato_Ritorna204() throws Exception {
        when(documentoService.findById(1)).thenReturn(Optional.of(new Documento()));

        mockMvc.perform(delete("/api/documenti/1"))
                .andExpect(status().isNoContent());

        verify(documentoService).eliminaDocumento(1);
    }

    @Test
    void getRinnoviByStato_RitornaLista200() throws Exception {
        RichiestaRinnovoDocumento rinnovo = new RichiestaRinnovoDocumento();
        RichiestaRinnovoDocumentoDTO dto = new RichiestaRinnovoDocumentoDTO();
        dto.setIdRichiesta(5);

        when(rinnovoService.trovaPerStato(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA)).thenReturn(List.of(rinnovo));
        when(rinnovoService.convertToDTO(rinnovo)).thenReturn(dto);

        mockMvc.perform(get("/api/documenti/rinnovi/stato/IN_ATTESA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRichiesta").value(5));
    }

    @Test
    void createRichiestaRinnovo_DocumentoTrovato_Ritorna201() throws Exception {
        Documento doc = new Documento();
        RichiestaRinnovoDocumento rinnovoSalvato = new RichiestaRinnovoDocumento();
        RichiestaRinnovoDocumentoDTO dto = new RichiestaRinnovoDocumentoDTO();
        dto.setIdRichiesta(7);

        when(documentoService.findById(1)).thenReturn(Optional.of(doc));
        when(rinnovoService.creaRichiesta(any(RichiestaRinnovoDocumento.class))).thenReturn(rinnovoSalvato);
        when(rinnovoService.convertToDTO(rinnovoSalvato)).thenReturn(dto);

        mockMvc.perform(post("/api/documenti/1/rinnovi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RichiestaRinnovoDocumento())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRichiesta").value(7));
    }

    @Test
    void updateStatoRinnovo_Trovata_Ritorna200() throws Exception {
        when(rinnovoService.findById(1)).thenReturn(Optional.of(new RichiestaRinnovoDocumento()));

        mockMvc.perform(patch("/api/documenti/rinnovi/1/stato")
                        .param("nuovoStato", "COMPLETATO"))
                .andExpect(status().isOk());

        verify(rinnovoService).cambiaStato(1, RichiestaRinnovoDocumento.StatoRinnovo.COMPLETATO);
    }
}