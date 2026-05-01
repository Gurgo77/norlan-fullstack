package it.norlan.clientportal.controller;

import it.norlan.clientportal.service.LogSincronizzazioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private LogSincronizzazioneService logService;

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex) {

        logService.registraEvento(
                "Errore I/O caricamento file",
                false,
                "Eccezione durante l'operazione sui file. Dettaglio: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Errore durante il salvataggio o la lettura del file.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityException(DataIntegrityViolationException ex) {

        String causa = (ex.getRootCause() != null) ? ex.getRootCause().getMessage() : ex.getMessage();

        logService.registraEvento(
                "Blocco sicurezza: Violazione integrità dati",
                false,
                "Eccezione database: Foreign Key Constraint o Unique Constraint fallito. Dettaglio tecnico: " + causa
        );

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Operazione bloccata dal database (probabile violazione di vincoli relazionali).");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleLogicalExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
