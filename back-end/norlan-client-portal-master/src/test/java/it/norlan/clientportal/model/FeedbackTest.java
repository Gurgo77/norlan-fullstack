package it.norlan.clientportal.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    private Feedback feedback;

    @BeforeEach
    void setUp() {
        feedback = new Feedback();

        feedback.setRatingDocenza(5);

        feedback.setRatingContenuti(5);

    }

    @Test
    void costruttore_InizializzaDataCreazioneCorrettamente() {
        assertNotNull(feedback.getDataCreazione(), "La data di creazione non può essere nulla");

    }

    @Test
    void prePersistValidation_RatingValidi_NonLanciaEccezione() {
        feedback.setRatingDocenza(1);

        feedback.setRatingContenuti(5);


        assertDoesNotThrow(() -> feedback.prePersistValidation(), "I rating devono essere un valore compreso tra 1 e 5");

    }

    @Test
    void prePersistValidation_RatingDocenzaMinoreDi1_LanciaEccezione() {
        feedback.setRatingDocenza(0);


        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> feedback.prePersistValidation()

        );
        assertEquals("I valori di rating devono essere compresi tra 1 e 5.", exception.getMessage());
    }

    @Test
    void prePersistValidation_RatingDocenzaMaggioreDi5_LanciaEccezione() {
        feedback.setRatingDocenza(6);


        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> feedback.prePersistValidation()

        );
        assertEquals("I valori di rating devono essere compresi tra 1 e 5.", exception.getMessage());
    }

    @Test
    void prePersistValidation_RatingContenutiMinoreDi1_LanciaEccezione() {
        feedback.setRatingContenuti(0);


        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> feedback.prePersistValidation()

        );
        assertEquals("I valori di rating devono essere compresi tra 1 e 5.", exception.getMessage());
    }

    @Test
    void prePersistValidation_RatingContenutiMaggioreDi5_LanciaEccezione() {
        feedback.setRatingContenuti(6);


        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> feedback.prePersistValidation()

        );
        assertEquals("I valori di rating devono essere compresi tra 1 e 5.", exception.getMessage());
    }

    @Test
    void propertySettersGetters_FunzionanoCorrettamente() {
        IscrizioneCorso iscrizioneMock = new IscrizioneCorso();
        LocalDateTime ora = LocalDateTime.now();

        feedback.setIdFeedback(100);

        feedback.setIscrizione(iscrizioneMock);

        feedback.setCommento("Corso eccellente e molto formativo");

        feedback.setDataCreazione(ora);


        assertEquals(100, feedback.getIdFeedback());

        assertEquals(iscrizioneMock, feedback.getIscrizione());

        assertEquals("Corso eccellente e molto formativo", feedback.getCommento());

        assertEquals(ora, feedback.getDataCreazione());

    }
}
