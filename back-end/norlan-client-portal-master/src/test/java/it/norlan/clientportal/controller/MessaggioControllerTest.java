package it.norlan.clientportal.controller;
import it.norlan.clientportal.dto.MessaggioDTO;
import it.norlan.clientportal.service.MessaggioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * Suite di collaudo (Unit Test) per l'API REST di consultazione dello storico comunicazioni.
 * Complementare all'infrastruttura Real-Time (WebSocket), verifica l'estrazione e la corretta
 * serializzazione della cronologia messaggi, validando gli alberi JSON tramite espressioni JsonPath.
 */
@ExtendWith(MockitoExtension.class)
class MessaggioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MessaggioService messaggioService;

    @InjectMocks
    private MessaggioController messaggioController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messaggioController).build();
    }

    // Collauda l'esposizione delle collezioni (Array JSON): simula il recupero del thread di conversazione e ispeziona sequenzialmente i nodi del DOM per garantirne l'integrità
    @Test
    void getCronologia_ParametriValidi_RitornaLista200() throws Exception {
        MessaggioDTO msg1 = new MessaggioDTO();
        msg1.setIdMessaggio(100);
        msg1.setTesto("Ciao");

        MessaggioDTO msg2 = new MessaggioDTO();
        msg2.setIdMessaggio(101);
        msg2.setTesto("Buongiorno");

        when(messaggioService.getCronologia(1, 2)).thenReturn(List.of(msg1, msg2));

        mockMvc.perform(get("/api/chat/cronologia/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idMessaggio").value(100))
                .andExpect(jsonPath("$[0].testo").value("Ciao"))
                .andExpect(jsonPath("$[1].idMessaggio").value(101))
                .andExpect(jsonPath("$[1].testo").value("Buongiorno"));
    }

    // Boundary Testing (Casi limite): assicura il rispetto delle convenzioni API REST garantendo che un set di risultati vuoto produca una risposta formattata formalmente (Array vuoto) e non un'eccezione
    @Test
    void getCronologia_CronologiaVuota_RitornaListaVuota200() throws Exception {
        when(messaggioService.getCronologia(1, 2)).thenReturn(List.of());

        mockMvc.perform(get("/api/chat/cronologia/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
