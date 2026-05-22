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
/**
 * Suite di collaudo (Unit Test) per il motore di calcolo del Rischio Aziendale (Compliance Rule Engine).
 * Verifica l'algoritmo di aggregazione delle scadenze documentali, validando le transizioni di stato
 * (RAG Status: Red, Amber, Green) basate su partizioni di equivalenza temporali (KPI Normativi).
 */
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

    // Time-Based Boundary Analysis (Soglia Critica): collauda la reattività del motore di compliance garantendo l'innesco dell'allerta massima (Stato ROSSO) per violazioni normative già consolidate
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

    // Equivalence Partitioning (Fasce di Rischio): verifica la classificazione preventiva del Warning normativo (Stato GIALLO) per i documenti che entrano nella finestra di tolleranza a 30 giorni
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

    // Baseline Validation (Happy Path): assicura che un set documentale con orizzonte temporale sicuro (Safe Zone) produca un aggregato di conformità totale, escludendo falsi positivi
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
