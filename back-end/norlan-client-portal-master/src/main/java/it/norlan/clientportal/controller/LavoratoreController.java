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

    @GetMapping
    public ResponseEntity<List<DipendenteDTO>> getAllDipendenti() {
        List<DipendenteDTO> dipendenti = dipendenteService.findAll()
                .stream()
                .map(dipendenteService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dipendenti);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DipendenteDTO> getDipendenteById(@PathVariable Integer id) {
        return dipendenteService.findById(id)
                .map(dip -> ResponseEntity.ok(dipendenteService.convertToDTO(dip)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/azienda/{idAzienda}")
    public ResponseEntity<List<DipendenteDTO>> getDipendentiByAzienda(@PathVariable Integer idAzienda) {
        List<DipendenteDTO> dipendenti = dipendenteService.findByAzienda(idAzienda)
                .stream()
                .map(dipendenteService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dipendenti);
    }

    @PostMapping("/azienda/{idAzienda}")
    public ResponseEntity<DipendenteDTO> createDipendente(@PathVariable Integer idAzienda, @RequestBody Dipendente dipendente) {
        dipendente.setRuolo(Utente.Ruolo.DIPENDENTE);
        try {
            Dipendente salvato = dipendenteService.salvaDipendente(dipendente, idAzienda);
            return new ResponseEntity<>(dipendenteService.convertToDTO(salvato), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DipendenteDTO> updateDipendente(@PathVariable Integer id, @RequestBody Dipendente dipendenteDati) {
        return dipendenteService.findById(id).map(dipendenteEsistente -> {
            dipendenteEsistente.setNome(dipendenteDati.getNome());
            dipendenteEsistente.setCognome(dipendenteDati.getCognome());
            dipendenteEsistente.setCodiceFiscale(dipendenteDati.getCodiceFiscale());

            if (dipendenteDati.getEmail() != null) {
                dipendenteEsistente.setEmail(dipendenteDati.getEmail());
            }

            Dipendente aggiornato = dipendenteService.salvaDipendente(
                    dipendenteEsistente,
                    dipendenteEsistente.getAzienda().getIdUtente()
            );

            return ResponseEntity.ok(dipendenteService.convertToDTO(aggiornato));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDipendente(@PathVariable Integer id) {
        return dipendenteService.findById(id).map(dipendente -> {
            dipendenteService.eliminaDipendente(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{idDipendente}/dpi")
    public ResponseEntity<List<AssegnazioneDPIDTO>> getDpiByDipendente(@PathVariable Integer idDipendente) {
        List<AssegnazioneDPIDTO> dpi = dpiService.trovaPerDipendente(idDipendente)
                .stream()
                .map(dpiService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dpi);
    }

    @PostMapping("/{idDipendente}/dpi")
    public ResponseEntity<AssegnazioneDPIDTO> assignDpiToDipendente(@PathVariable Integer idDipendente, @RequestBody AssegnazioneDPI assegnazione) {
        return dipendenteService.findById(idDipendente).map(dipendente -> {
            assegnazione.setDipendente(dipendente);
            AssegnazioneDPI salvata = dpiService.salvaAssegnazione(assegnazione);
            return new ResponseEntity<>(dpiService.convertToDTO(salvata), HttpStatus.CREATED);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dpi/in-scadenza")
    public ResponseEntity<List<AssegnazioneDPIDTO>> getDpiInScadenza(@RequestParam(defaultValue = "30") int giorni) {
        List<AssegnazioneDPIDTO> dpiInScadenza = dpiService.trovaInScadenzaRevisione(giorni)
                .stream()
                .map(dpiService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dpiInScadenza);
    }

    @DeleteMapping("/dpi/{idDpi}")
    public ResponseEntity<Void> deleteDpiAssignment(@PathVariable Integer idDpi) {
        return dpiService.findById(idDpi).map(dpi -> {
            dpiService.eliminaAssegnazione(idDpi);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
