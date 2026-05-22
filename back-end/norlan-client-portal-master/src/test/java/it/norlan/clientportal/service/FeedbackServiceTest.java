package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.FeedbackDTO;
import it.norlan.clientportal.dto.FeedbackStatsDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.Feedback;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.repository.FeedbackRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di Business Logic delle recensioni didattiche.
 * Verifica il rigoroso rispetto dei vincoli di dominio (Precondizioni di partecipazione),
 * l'idempotenza delle sottomissioni e l'aggregazione algoritmica delle metriche di Analytics (KPI).
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private IscrizioneCorsoRepository iscrizioneRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private FeedbackDTO feedbackDTO;
    private IscrizioneCorso iscrizione;
    private CorsoFormazione corso;
    private IscrizioneCorso.IscrizioneId iscrizioneId;

    @BeforeEach
    void setUp() {

        feedbackDTO = new FeedbackDTO();
        feedbackDTO.setIdUtente(1);
        feedbackDTO.setIdCorso(10);
        feedbackDTO.setRatingDocenza(4);
        feedbackDTO.setRatingContenuti(5);
        feedbackDTO.setCommento("Ottimo corso");

        iscrizioneId = new IscrizioneCorso.IscrizioneId(1, 10);

        corso = new CorsoFormazione();
        corso.setIdCorso(10);
        corso.setStato(CorsoFormazione.StatoCorso.CONCLUSO);

        iscrizione = new IscrizioneCorso();
        iscrizione.setId(iscrizioneId);
        iscrizione.setCorso(corso);
        iscrizione.setPresenzaConfermata(true);
    }

    @Test
    void registraFeedback_IscrizioneNonTrovata_LanciaEccezione() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> feedbackService.registraFeedback(feedbackDTO));
        verify(feedbackRepository, never()).save(any());
    }

    // Validazione degli Invarianti di Dominio (State & Preconditions): assicura che il sistema rigetti sottomissioni per corsi non terminati o per utenti privi di presenza validata
    @Test
    void registraFeedback_CorsoProgrammato_LanciaEccezione() {
        corso.setStato(CorsoFormazione.StatoCorso.PROGRAMMATO);
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        assertThrows(IllegalStateException.class, () -> feedbackService.registraFeedback(feedbackDTO));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void registraFeedback_CorsoInSvolgimento_LanciaEccezione() {
        corso.setStato(CorsoFormazione.StatoCorso.IN_SVOLGIMENTO);
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        assertThrows(IllegalStateException.class, () -> feedbackService.registraFeedback(feedbackDTO));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void registraFeedback_PresenzaNull_LanciaEccezione() {
        iscrizione.setPresenzaConfermata(null);
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        assertThrows(IllegalStateException.class, () -> feedbackService.registraFeedback(feedbackDTO));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void registraFeedback_PresenzaNonConfermata_LanciaEccezione() {
        iscrizione.setPresenzaConfermata(false);
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));

        assertThrows(IllegalStateException.class, () -> feedbackService.registraFeedback(feedbackDTO));
        verify(feedbackRepository, never()).save(any());
    }

    // Anti-Spam & Idempotency: collauda il blocco architetturale contro le sottomissioni multiple, garantendo l'integrità matematica e statistica delle medie di valutazione
    @Test
    void registraFeedback_FeedbackGiaPresente_LanciaEccezione() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));
        when(feedbackRepository.findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(1, 10)).thenReturn(Optional.of(new Feedback()));

        assertThrows(IllegalStateException.class, () -> feedbackService.registraFeedback(feedbackDTO));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void registraFeedback_DatiValidi_SalvaCorrettamente() {
        when(iscrizioneRepository.findById(iscrizioneId)).thenReturn(Optional.of(iscrizione));
        when(feedbackRepository.findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(1, 10)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));

        Feedback result = feedbackService.registraFeedback(feedbackDTO);

        assertNotNull(result);
        assertEquals(4, result.getRatingDocenza());
        assertEquals(5, result.getRatingContenuti());
        assertEquals("Ottimo corso", result.getCommento());
        assertEquals(iscrizione, result.getIscrizione());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void getStatisticheCorso_NessunFeedback_RitornaStatisticheVuote() {
        when(feedbackRepository.findByIscrizione_Id_IdCorso(10)).thenReturn(Collections.emptyList());

        FeedbackStatsDTO result = feedbackService.getStatisticheCorso(10);

        assertEquals(10, result.getIdCorso());
        assertEquals(0.0, result.getMediaDocenza());
        assertEquals(0.0, result.getMediaContenuti());
        assertEquals(0L, result.getTotaleFeedback());
        assertTrue(result.getCommenti().isEmpty());
    }

    // Data Aggregation & Analytics: verifica l'algoritmo di proiezione dei dati per le dashboard, validando il calcolo delle medie aritmetiche (Floating point) e il filtraggio dei testi vuoti
    @Test
    void getStatisticheCorso_ConFeedback_CalcolaMedieECommenti() {
        Feedback f1 = new Feedback();
        f1.setRatingDocenza(4);
        f1.setRatingContenuti(5);
        f1.setCommento("Buono");

        Feedback f2 = new Feedback();
        f2.setRatingDocenza(3);
        f2.setRatingContenuti(4);
        f2.setCommento("Nella media");

        Feedback f3 = new Feedback();
        f3.setRatingDocenza(5);
        f3.setRatingContenuti(4);
        f3.setCommento("");

        when(feedbackRepository.findByIscrizione_Id_IdCorso(10)).thenReturn(List.of(f1, f2, f3));

        FeedbackStatsDTO result = feedbackService.getStatisticheCorso(10);

        assertEquals(10, result.getIdCorso());
        assertEquals(4.0, result.getMediaDocenza(), 0.01);
        assertEquals(4.3, result.getMediaContenuti(), 0.01);
        assertEquals(3L, result.getTotaleFeedback());
        assertEquals(2, result.getCommenti().size());
        assertTrue(result.getCommenti().contains("Buono"));
        assertTrue(result.getCommenti().contains("Nella media"));
    }
}
