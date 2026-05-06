package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.ComplianceDTO;
import it.norlan.clientportal.model.LivelloRischio;
import it.norlan.clientportal.service.ComplianceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComplianceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private ComplianceController complianceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(complianceController).build();
    }

    @Test
    void getComplianceStatus_RitornaComplianceDTOEStatus200() throws Exception {
        Integer idAzienda = 1;
        ComplianceDTO mockResponse = new ComplianceDTO(
                LivelloRischio.VERDE,
                0,
                0,
                "Conformit  garantita: tutti i documenti sono validi."
        );

        when(complianceService.calcolaComplianceAzienda(idAzienda)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/compliance/azienda/{idAzienda}", idAzienda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statoGlobale").value("VERDE"))
                .andExpect(jsonPath("$.documentiScaduti").value(0))
                .andExpect(jsonPath("$.documentiInScadenza").value(0))
                .andExpect(jsonPath("$.messaggioSuggerimento").value("Conformit  garantita: tutti i documenti sono validi."));
    }
}
