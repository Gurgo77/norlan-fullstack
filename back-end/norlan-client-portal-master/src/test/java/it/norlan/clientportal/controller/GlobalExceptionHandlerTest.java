package it.norlan.clientportal.controller;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
/**
 * Suite di collaudo (Unit Test) per l'intercettore globale delle eccezioni (AOP - Aspect-Oriented Programming).
 * Valida la centralizzazione dell'Error Handling, garantendo che le anomalie di sistema vengano
 * tradotte in risposte HTTP semantiche (Fault Tolerance) mascherando i dettagli tecnici agli utenti (Sicurezza).
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
    }

    // Valida la gestione degli errori I/O (es. File System): verifica che lo Stack Trace originale venga soppresso e sostituito da un HTTP 500 generico, delegando il tracciamento al log interno
    @Test
    void handleIOException_Ritorna500ELoggaErrore() {
        IOException eccezione = new IOException("File non trovato");

        ResponseEntity<String> response = globalExceptionHandler.handleIOException(eccezione);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Errore durante il salvataggio o la lettura del file.", response.getBody());

        verify(logService).registraEvento(
                eq("Errore I/O caricamento file"),
                eq(false),
                eq("Eccezione durante l'operazione sui file. Dettaglio: File non trovato")
        );
    }

    // Collauda la mitigazione degli errori critici di persistenza (Database): assicura che le violazioni relazionali generino un HTTP 409 (Conflict) e che la Root Cause venga isolata nel sistema di Auditing
    @Test
    void handleDataIntegrityException_SenzaRootCause_Ritorna409ELoggaCausaDiretta() {
        DataIntegrityViolationException eccezione = new DataIntegrityViolationException("Violazione vincolo unico");

        ResponseEntity<String> response = globalExceptionHandler.handleDataIntegrityException(eccezione);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Operazione bloccata dal database (probabile violazione di vincoli relazionali).", response.getBody());
        verify(logService).registraEvento(
                eq("Blocco sicurezza: Violazione integrità dati"),
                eq(false),
                eq("Eccezione database: Foreign Key Constraint o Unique Constraint fallito. Dettaglio tecnico: Violazione vincolo unico")
        );
    }

    @Test
    void handleDataIntegrityException_ConRootCause_Ritorna409ELoggaRootCause() {
        Throwable rootCause = new RuntimeException("Causa radice del database");
        DataIntegrityViolationException eccezione = new DataIntegrityViolationException("Violazione vincolo unico", rootCause);

        ResponseEntity<String> response = globalExceptionHandler.handleDataIntegrityException(eccezione);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Operazione bloccata dal database (probabile violazione di vincoli relazionali).", response.getBody());

        verify(logService).registraEvento(
                eq("Blocco sicurezza: Violazione integrità dati"),
                eq(false),
                eq("Eccezione database: Foreign Key Constraint o Unique Constraint fallito. Dettaglio tecnico: Causa radice del database")
        );
    }

    // Prevenzione del Log Flooding: garantisce che i comuni errori di validazione del client (HTTP 400) non vengano persistiti su disco, proteggendo lo storage del server da saturazione dolosa
    @Test
    void handleLogicalExceptions_IllegalArgumentException_Ritorna400SenzaLog() {
        IllegalArgumentException eccezione = new IllegalArgumentException("Parametro non valido");

        ResponseEntity<String> response = globalExceptionHandler.handleLogicalExceptions(eccezione);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parametro non valido", response.getBody());

        verifyNoInteractions(logService);
    }

    @Test
    void handleLogicalExceptions_IllegalStateException_Ritorna400SenzaLog() {
        IllegalStateException eccezione = new IllegalStateException("Stato non consentito");

        ResponseEntity<String> response = globalExceptionHandler.handleLogicalExceptions(eccezione);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Stato non consentito", response.getBody());

        verifyNoInteractions(logService);
    }
}
