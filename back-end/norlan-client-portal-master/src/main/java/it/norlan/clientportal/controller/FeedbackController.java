package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.FeedbackDTO;
import it.norlan.clientportal.dto.FeedbackStatsDTO;
import it.norlan.clientportal.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

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
