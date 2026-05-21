package it.norlan.clientportal.controller;

import it.norlan.clientportal.service.LogSincronizzazioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.io.IOException;

/**
 * Intercettore globale delle eccezioni di sistema.
 * Cattura in modo centralizzato gli errori (I/O, Database, Logica di business)
 * generati dai vari Controller, restituendo risposte HTTP standardizzate e loggando i fallimenti.
 */

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private LogSincronizzazioneService logService;

    // Gestisce gli errori di lettura/scrittura su disco (es. upload di file falliti) e li traccia nei log
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

    // Cattura i conflitti sul database (es. email duplicate o dipendenze vincolate) prevenendo il crash dell'applicazione
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

    // Intercetta gli errori di validazione e di stato irregolare, informando il client con un Bad Request (400)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleLogicalExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
