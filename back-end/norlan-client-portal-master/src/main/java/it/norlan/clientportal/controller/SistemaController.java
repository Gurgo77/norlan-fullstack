package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.LogSincronizzazioneDTO;
import it.norlan.clientportal.dto.NotificaDTO;
import it.norlan.clientportal.model.LogSincronizzazione;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import it.norlan.clientportal.service.NotificaService;
// Nota: Assicurati di avere un UtenteService o un generico repository per recuperare il destinatario
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sistema")
@CrossOrigin(origins = "*")
public class SistemaController {

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private LogSincronizzazioneService logService;

    // ==========================================
    // SEZIONE 1: NOTIFICHE
    // ==========================================

    @GetMapping("/notifiche/utente/{idUtente}")
    public ResponseEntity<List<NotificaDTO>> getNotificheUtente(@PathVariable Integer idUtente) {
        List<NotificaDTO> notifiche = notificaService.getNotificheUtente(idUtente)
                .stream()
                .map(notificaService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifiche);
    }

    @GetMapping("/notifiche/utente/{idUtente}/non-lette/count")
    public ResponseEntity<Long> contaNotificheNonLette(@PathVariable Integer idUtente) {
        long conteggio = notificaService.contaNonLette(idUtente);
        return ResponseEntity.ok(conteggio);
    }

    @PatchMapping("/notifiche/{idNotifica}/letta")
    public ResponseEntity<Void> segnaNotificaComeLetta(@PathVariable Integer idNotifica) {
        // Presuppone che NotificaService abbia un metodo existsById o gestisca l'id in modo sicuro
        notificaService.segnaComeLetta(idNotifica);
        return ResponseEntity.ok().<Void>build();
    }

    @DeleteMapping("/notifiche/{idNotifica}")
    public ResponseEntity<Void> deleteNotifica(@PathVariable Integer idNotifica) {
        // Richiederà l'aggiunta di eliminaNotifica nel Service
        notificaService.eliminaNotifica(idNotifica);
        return ResponseEntity.noContent().<Void>build();
    }

    // ==========================================
    // SEZIONE 2: LOG E MONITORAGGIO
    // ==========================================

    @GetMapping("/logs")
    public ResponseEntity<List<LogSincronizzazioneDTO>> getAllLogs() {
        List<LogSincronizzazioneDTO> logs = logService.findAll()
                .stream()
                .map(logService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/errori")
    public ResponseEntity<List<LogSincronizzazioneDTO>> getErrorLogs() {
        List<LogSincronizzazioneDTO> errori = logService.trovaErrori()
                .stream()
                .map(logService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(errori);
    }

    @PostMapping("/logs")
    public ResponseEntity<LogSincronizzazioneDTO> createLog(@RequestBody LogSincronizzazione log) {
        LogSincronizzazione salvato = logService.registraEvento(
                log.getDescrizioneEvento(),
                log.getEsitoPositivo(),
                log.getNoteTecniche()
        );
        return new ResponseEntity<>(logService.convertToDTO(salvato), HttpStatus.CREATED);
    }

    @DeleteMapping("/logs/pulizia")
    public ResponseEntity<Void> pulisciLogVecchi(@RequestParam(defaultValue = "30") int giorniVecchiaia) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(giorniVecchiaia);
        logService.pulisciLogVecchi(dataLimite);
        return ResponseEntity.noContent().<Void>build();
    }
}
