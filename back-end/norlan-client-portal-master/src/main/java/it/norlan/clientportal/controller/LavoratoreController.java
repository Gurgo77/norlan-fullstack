package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.AssegnazioneDPIDTO;
import it.norlan.clientportal.dto.DipendenteDTO;
import it.norlan.clientportal.model.AssegnazioneDPI;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.service.AssegnazioneDPIService;
import it.norlan.clientportal.service.DipendenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lavoratori")
@CrossOrigin(origins = "*")
public class LavoratoreController {

    @Autowired
    private DipendenteService dipendenteService;

    @Autowired
    private AssegnazioneDPIService dpiService;

    // ==========================================
    // SEZIONE DIPENDENTI (Risorsa principale)
    // ==========================================

    /**
     * Recupera tutti i dipendenti del sistema (Utile per dashboard globale)
     */
    @GetMapping
    public ResponseEntity<List<DipendenteDTO>> getAllDipendenti() {
        List<DipendenteDTO> dipendenti = dipendenteService.findAll()
                .stream()
                .map(dipendenteService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dipendenti);
    }

    /**
     * Recupera un dipendente specifico tramite ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DipendenteDTO> getDipendenteById(@PathVariable Integer id) {
        return dipendenteService.findById(id)
                .map(dip -> ResponseEntity.ok(dipendenteService.convertToDTO(dip)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Filtra i dipendenti appartenenti a una specifica azienda
     */
    @GetMapping("/azienda/{idAzienda}")
    public ResponseEntity<List<DipendenteDTO>> getDipendentiByAzienda(@PathVariable Integer idAzienda) {
        List<DipendenteDTO> dipendenti = dipendenteService.findByAzienda(idAzienda)
                .stream()
                .map(dipendenteService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dipendenti);
    }

    /**
     * Crea un nuovo dipendente associandolo all'azienda specificata
     */
    @PostMapping("/azienda/{idAzienda}")
    public ResponseEntity<DipendenteDTO> createDipendente(@PathVariable Integer idAzienda, @RequestBody Dipendente dipendente) {
        dipendente.setRuolo(Utente.Ruolo.DIPENDENTE);
        try {
            Dipendente salvato = dipendenteService.salvaDipendente(dipendente, idAzienda);
            return new ResponseEntity<>(dipendenteService.convertToDTO(salvato), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build(); // Ritorna 400 se l'azienda non esiste o il CF è errato
        }
    }

    /**
     * Aggiorna i dati anagrafici di un dipendente
     */
    @PutMapping("/{id}")
    public ResponseEntity<DipendenteDTO> updateDipendente(@PathVariable Integer id, @RequestBody Dipendente dipendenteDati) {
        return dipendenteService.findById(id).map(dipendenteEsistente -> {
            // Aggiornamento campi anagrafici (Dati specifici di Dipendente)
            dipendenteEsistente.setNome(dipendenteDati.getNome());
            dipendenteEsistente.setCognome(dipendenteDati.getCognome());
            dipendenteEsistente.setCodiceFiscale(dipendenteDati.getCodiceFiscale());

            // Aggiornamento email (Dato ereditato da Utente)
            if (dipendenteDati.getEmail() != null) {
                dipendenteEsistente.setEmail(dipendenteDati.getEmail());
            }

            // Salvataggio tramite Service (mantenendo l'associazione con l'azienda attuale)
            Dipendente aggiornato = dipendenteService.salvaDipendente(
                    dipendenteEsistente,
                    dipendenteEsistente.getAzienda().getIdUtente()
            );

            return ResponseEntity.ok(dipendenteService.convertToDTO(aggiornato));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rimuove un dipendente dal sistema
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDipendente(@PathVariable Integer id) {
        return dipendenteService.findById(id).map(dipendente -> {
            dipendenteService.eliminaDipendente(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // SEZIONE DPI (Sotto-risorsa nidificata)
    // ==========================================

    /**
     * Recupera tutti i DPI assegnati a un lavoratore specifico
     */
    @GetMapping("/{idDipendente}/dpi")
    public ResponseEntity<List<AssegnazioneDPIDTO>> getDpiByDipendente(@PathVariable Integer idDipendente) {
        List<AssegnazioneDPIDTO> dpi = dpiService.trovaPerDipendente(idDipendente)
                .stream()
                .map(dpiService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dpi);
    }

    /**
     * Assegna un nuovo DPI a un lavoratore specifico
     */
    @PostMapping("/{idDipendente}/dpi")
    public ResponseEntity<AssegnazioneDPIDTO> assignDpiToDipendente(@PathVariable Integer idDipendente, @RequestBody AssegnazioneDPI assegnazione) {
        return dipendenteService.findById(idDipendente).map(dipendente -> {
            assegnazione.setDipendente(dipendente); // Colleghiamo fisicamente l'entità
            AssegnazioneDPI salvata = dpiService.salvaAssegnazione(assegnazione);
            return new ResponseEntity<>(dpiService.convertToDTO(salvata), HttpStatus.CREATED);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint di monitoraggio: Restituisce tutti i DPI in scadenza entro X giorni
     */
    @GetMapping("/dpi/in-scadenza")
    public ResponseEntity<List<AssegnazioneDPIDTO>> getDpiInScadenza(
            @RequestParam(defaultValue = "30") int giorni) { // Se non specificato, cerca quelli in scadenza nei prossimi 30 giorni

        List<AssegnazioneDPIDTO> dpiInScadenza = dpiService.trovaInScadenzaRevisione(giorni)
                .stream()
                .map(dpiService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dpiInScadenza);
    }

    /**
     * Rimuove o invalida un'assegnazione DPI errata
     */
    @DeleteMapping("/dpi/{idDpi}")
    public ResponseEntity<Void> deleteDpiAssignment(@PathVariable Integer idDpi) {
        return dpiService.findById(idDpi).map(dpi -> {
            dpiService.eliminaAssegnazione(idDpi);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
