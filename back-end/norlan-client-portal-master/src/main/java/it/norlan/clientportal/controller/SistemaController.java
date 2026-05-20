package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.LogSincronizzazioneDTO;
import it.norlan.clientportal.dto.NotificaDTO;
import it.norlan.clientportal.model.LogSincronizzazione;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import it.norlan.clientportal.service.NotificaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST per l'infrastruttura di sistema e l'auditing.
 * Gestisce l'intero ciclo di vita delle notifiche push/in-app degli utenti
 * e l'esposizione e manutenzione dei log di sicurezza ed errore (Auditing).
 */

@RestController
@RequestMapping("/api/sistema")
@CrossOrigin(origins = "*")
public class SistemaController {

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private LogSincronizzazioneService logService;

    // Endpoint per il recupero delle notifiche (totali e da leggere) e per alimentare i counter dell'interfaccia utente
    @GetMapping("/notifiche/utente/{idUtente}")
    public ResponseEntity<List<NotificaDTO>> getNotificheUtente(@PathVariable Integer idUtente) {
        List<NotificaDTO> notifiche = notificaService.getNotificheUtente(idUtente)
                .stream()
                .map(notificaService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifiche);
    }

    @GetMapping("/notifiche/utente/{idUtente}/non-lette")
    public ResponseEntity<List<NotificaDTO>> getNotificheNonLette(@PathVariable Integer idUtente) {
        List<NotificaDTO> notifiche = notificaService.getNotificheNonLette(idUtente)
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
    // Gestisce il cambio di stato della notifica (es. segna come letta) e la sua eliminazione dal database
    @PatchMapping("/notifiche/{idNotifica}/letta")
    public ResponseEntity<Void> segnaNotificaComeLetta(@PathVariable Integer idNotifica) {
        notificaService.segnaComeLetta(idNotifica);
        return ResponseEntity.ok().<Void>build();
    }

    @DeleteMapping("/notifiche/{idNotifica}")
    public ResponseEntity<Void> deleteNotifica(@PathVariable Integer idNotifica) {
        notificaService.eliminaNotifica(idNotifica);
        return ResponseEntity.noContent().<Void>build();
    }

    // Endpoint dedicati all'auditing: lettura dello storico eventi di sistema, monitoraggio degli errori e creazione log
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

    // Operazione di manutenzione: elimina i log obsoleti antecedenti a una determinata soglia di tempo per liberare spazio
    @DeleteMapping("/logs/pulizia")
    public ResponseEntity<Void> pulisciLogVecchi(@RequestParam(defaultValue = "30") int giorniVecchiaia) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(giorniVecchiaia);
        logService.pulisciLogVecchi(dataLimite);
        return ResponseEntity.noContent().<Void>build();
    }
}
