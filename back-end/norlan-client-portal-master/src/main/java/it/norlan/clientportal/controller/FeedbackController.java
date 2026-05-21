package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.FeedbackDTO;
import it.norlan.clientportal.dto.FeedbackStatsDTO;
import it.norlan.clientportal.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione dei feedback dei corsi di formazione.
 * Consente la raccolta dei moduli di valutazione inviati dagli utenti e l'estrazione
 * delle statistiche aggregate e delle metriche di gradimento per singolo corso.
 */

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    // Valida e registra un nuovo feedback inviato da un utente al termine di un corso
    @PostMapping("/invia")
    public ResponseEntity<?> inviaFeedback(@Valid @RequestBody FeedbackDTO feedbackDTO) {

        try {
            feedbackService.registraFeedback(feedbackDTO);
            return ResponseEntity.ok().body("Feedback archiviato con successo e metriche aggiornate.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Errore di sistema durante il salvataggio del feedback.");
        }
    }

    // Recupera i dati aggregati e le medie dei punteggi di valutazione per un determinato corso
    @GetMapping("/corso/{idCorso}")
    public ResponseEntity<?> getStatisticheCorso(@PathVariable Integer idCorso) {
        try {
            FeedbackStatsDTO stats = feedbackService.getStatisticheCorso(idCorso);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Errore durante il recupero delle statistiche del feedback.");
        }
    }
}
