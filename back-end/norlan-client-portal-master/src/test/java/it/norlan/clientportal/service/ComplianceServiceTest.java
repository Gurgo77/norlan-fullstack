package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.ComplianceDTO;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.LivelloRischio;
import it.norlan.clientportal.repository.DocumentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @InjectMocks
    private ComplianceService complianceService;

    private final Integer idAzienda = 1;

    @Test
    void calcolaComplianceAzienda_NessunDocumento_RitornaVerde() {
        when(documentoRepository.findByAziendaIdUtente(idAzienda)).thenReturn(Collections.emptyList());

        ComplianceDTO result = complianceService.calcolaComplianceAzienda(idAzienda);

        assertEquals(LivelloRischio.VERDE, result.getStatoGlobale());
        assertEquals(0, result.getDocumentiScaduti());
        assertEquals(0, result.getDocumentiInScadenza());
        assertEquals("Conformità garantita: tutti i documenti sono validi.", result.getMessaggioSuggerimento());
    }

    @Test
    void calcolaComplianceAzienda_DocumentoScaduto_RitornaRosso() {
        Documento doc = new Documento();
        doc.setDataScadenza(LocalDate.now().minusDays(1));

        when(documentoRepository.findByAziendaIdUtente(idAzienda)).thenReturn(List.of(doc));

        ComplianceDTO result = complianceService.calcolaComplianceAzienda(idAzienda);

        assertEquals(LivelloRischio.ROSSO, result.getStatoGlobale());
        assertEquals(1, result.getDocumentiScaduti());
        assertEquals(0, result.getDocumentiInScadenza());
        assertEquals("Azione immediata richiesta: presenti documenti scaduti o in scadenza entro 7 giorni.", result.getMessaggioSuggerimento());
    }

    @Test
    void calcolaComplianceAzienda_DocumentoInScadenzaCritica_RitornaRosso() {
        Documento doc = new Documento();
        doc.setDataScadenza(LocalDate.now().plusDays(2));

        when(documentoRepository.findByAziendaIdUtente(idAzienda)).thenReturn(List.of(doc));

        ComplianceDTO result = complianceService.calcolaComplianceAzienda(idAzienda);

        assertEquals(LivelloRischio.ROSSO, result.getStatoGlobale());
        assertEquals(0, result.getDocumentiScaduti());
        assertEquals(1, result.getDocumentiInScadenza());
    }

    @Test
    void calcolaComplianceAzienda_DocumentoInScadenzaImminente_RitornaGiallo() {
        Documento doc = new Documento();
        doc.setDataScadenza(LocalDate.now().plusDays(15));

        when(documentoRepository.findByAziendaIdUtente(idAzienda)).thenReturn(List.of(doc));

        ComplianceDTO result = complianceService.calcolaComplianceAzienda(idAzienda);

        assertEquals(LivelloRischio.GIALLO, result.getStatoGlobale());
        assertEquals(0, result.getDocumentiScaduti());
        assertEquals(1, result.getDocumentiInScadenza());
        assertEquals("Attenzione: alcuni documenti scadranno nei prossimi 30 giorni.", result.getMessaggioSuggerimento());
    }

    @Test
    void calcolaComplianceAzienda_DocumentoValido_RitornaVerde() {
        Documento doc = new Documento();
        doc.setDataScadenza(LocalDate.now().plusDays(40));

        when(documentoRepository.findByAziendaIdUtente(idAzienda)).thenReturn(List.of(doc));

        ComplianceDTO result = complianceService.calcolaComplianceAzienda(idAzienda);

        assertEquals(LivelloRischio.VERDE, result.getStatoGlobale());
        assertEquals(0, result.getDocumentiScaduti());
        assertEquals(0, result.getDocumentiInScadenza());
    }
}
