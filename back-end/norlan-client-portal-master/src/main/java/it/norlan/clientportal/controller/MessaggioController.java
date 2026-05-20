package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.MessaggioDTO;
import it.norlan.clientportal.service.MessaggioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller REST dedicato al recupero dello storico della messaggistica.
 * Lavora in sinergia con i WebSocket: permette al frontend di caricare
 * le conversazioni pregresse tra due utenti all'apertura della chat.
 */

@RestController
@RequestMapping("/api/chat")
public class MessaggioController {

    @Autowired
    private MessaggioService messaggioService;

    // Recupera dal database l'intera cronologia dei messaggi scambiati tra due utenti specifici
    @GetMapping("/cronologia/{id1}/{id2}")
    public ResponseEntity<List<MessaggioDTO>> getCronologia(@PathVariable Integer id1, @PathVariable Integer id2) {
        return ResponseEntity.ok(messaggioService.getCronologia(id1, id2));
    }
}
