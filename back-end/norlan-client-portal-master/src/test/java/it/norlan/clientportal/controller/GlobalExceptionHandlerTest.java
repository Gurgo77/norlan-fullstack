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

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
    }

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

    @Test
    void handleDataIntegrityException_SenzaRootCause_Ritorna409ELoggaCausaDiretta() {
        DataIntegrityViolationException eccezione = new DataIntegrityViolationException("Violazione vincolo unico");

        ResponseEntity<String> response = globalExceptionHandler.handleDataIntegrityException(eccezione);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Operazione bloccata dal database perchè probabile violazione di vincoli relazionali.", response.getBody());

        verify(logService).registraEvento(
                eq("Blocco sicurezza: Violazione integrità dati"),
                eq(false),
                eq("Eccezione database, causa radice del database")
        );
    }

    @Test
    void handleDataIntegrityException_ConRootCause_Ritorna409ELoggaRootCause() {
        Throwable rootCause = new RuntimeException("Causa radice del database");
        DataIntegrityViolationException eccezione = new DataIntegrityViolationException("Violazione vincolo unico", rootCause);

        ResponseEntity<String> response = globalExceptionHandler.handleDataIntegrityException(eccezione);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Operazione bloccata dal database perchè probabile violazione di vincoli relazionali.", response.getBody());

        verify(logService).registraEvento(
                eq("Blocco sicurezza: Violazione integrità dati"),
                eq(false),
                eq("Eccezione database, causa radice del database")
        );
    }

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
